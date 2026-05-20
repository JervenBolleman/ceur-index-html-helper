package swiss.sib.swissprot.oxfabsceur;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "download-oa")
public class OxfordAbstractsDownloader implements Callable<Integer> {
	private static final Logger logger = LoggerFactory.getLogger(OxfordAbstractsDownloader.class);

	private static final String API_URL = "https://app.oxfordabstracts.com/v1/graphql";

	private static final String QUERY_GRAPHQL = """
			query subs { 
				events_by_pk(id: $eventId) { 
					id submissions { 
						id decision { value } 
						accepted_for { value } 
						title {without_html} 
						responses {value 
							question {
								question_name data_type api_name archived id 
								}
							}
						serial_number
					}
				}
			}
			""";
	
	@Option(names = "--download-dir-pdfs", description = "What will be the input directory containing a preface.pdf and directories for each section containing all the paper pdfs", required = true)
	Path pdfsDir;

	@Option(names = "--download-dir-agreements", description = "Where will you download the uploaded CEUR agreements to" , required = true)
	Path agreementsDir;

	@Option(names = "--api-token", required = true)
	private String authToken = "YOUR_API_TOKEN";

	@Option(names = "--event-id", required = true)
	private String eventId = "YOUR_EVENT_ID";

	@Option(names = "--pdf-field-name", description = "The name of the field that contains the PDFs that you are interested in", required = true)
	private String targetField = "Upload PDF"; // Change to your field name
	
	@Option(names = "--agreement-field-name", description = "The name of the field that contains the agreement PDFs", required = true)
	private String agreementField = "Copyright form CEUR"; // Change to your field name

	public Integer call() throws IOException {
		HttpClient client = HttpClient.newHttpClient();
		ObjectMapper mapper = new ObjectMapper();

		String jsonPayload = createGraphQLQuery(mapper);

		try {
			JsonNode submissions = sendRequests(client, mapper, jsonPayload);
			processAndDownload(client, submissions);
		} catch (InterruptedException e) {
			return 1;
		}
		return 0;
	}

	private String createGraphQLQuery(ObjectMapper mapper) throws JsonProcessingException {
		String qr = QUERY_GRAPHQL.replace("$eventId", eventId).replace('\n', ' ');
		return mapper
				.writeValueAsString(Map.of("query", qr, "variables", Map.of(), "operationName", "subs"));
	}

	private JsonNode sendRequests(HttpClient client, ObjectMapper mapper, String jsonPayload)
			throws IOException, InterruptedException, JsonProcessingException, JsonMappingException {
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL))
				.header("content-type", "application/json").header("x-api-key", authToken)
				.POST(BodyPublishers.ofString(jsonPayload)).build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode root = mapper.readTree(response.body());
		return root.path("data").path("events_by_pk").path("submissions");
	}

	private void processAndDownload(HttpClient client, JsonNode submissions) {
		for (JsonNode sub : submissions) {
			String subId = sub.path("id").asText();
			String status = sub.path("decision").path("value").asText("No_Decision");
			String session = sub.path("accepted_for").path("value").asText("Poster");
			String serialNumber = sub.path("serial_number").asText();
			for (JsonNode resp : sub.path("responses")) {
				String questionTitle = resp.path("question").path("question_name").asText();
				String fileUrl = resp.path("value").asText();

				if (targetField.equals(questionTitle)) {
					downloadSubmittedPdf(client, subId, status, session, serialNumber, questionTitle, fileUrl);
				} else if (agreementField.equals(questionTitle)) {
					downloadAgreement(client, subId, status, session, serialNumber, questionTitle, fileUrl);
				}
			}
		}
	}

	private void downloadSubmittedPdf(HttpClient client, String subId, String status, String session,
			String serialNumber, String questionTitle, String fileUrl) {
		if (fileUrl.isEmpty()) {
			logger.error("sub: {}, status:{}, question:{}, value is empty, session:{}", subId, status,
					questionTitle, session);
		} else {
			logger.info("sub: {}, status:{}, question:{}, value:{}, session:{}", subId, status,
					questionTitle, fileUrl, session);
			String extension = fileUrl.contains(".") ? fileUrl.substring(fileUrl.lastIndexOf(".")) : ".pdf";

			String fileName = "submission_" + serialNumber + extension;

			Path sd = pdfsDir.resolve(session);
			try {
				if (!Files.exists(sd)) {
					Files.createDirectories(sd);
				}
				downloadFile(client, fileUrl, fileName, status , sd);
			} catch (IOException e) {
				logger.error("Failed to create directory for session: " + session + "/" + sd, e);
			}
		}
	}

	private void downloadAgreement(HttpClient client, String subId, String status, String session, String serialNumber,
			String questionTitle, String fileUrl) {
		if (fileUrl.isEmpty()) {
			logger.error("No agreement for sub: {}, status:{}, serial_number:{}, value is empty, session:{}", subId, status,
					serialNumber, session);
		} else {
			logger.info("sub: {}, status:{}, question:{}, value:{}, session:{}", subId, status,
					questionTitle, fileUrl, session);
			String extension = fileUrl.contains(".") ? fileUrl.substring(fileUrl.lastIndexOf(".")) : ".pdf";
			String fileName = "agreement_" + serialNumber + extension;

			downloadFile(client, fileUrl, fileName, status , agreementsDir);
		}
	}

	private void downloadFile(HttpClient client, String url, String fileName, String status, Path pdfsDir) {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() == 200) {
				Files.copy(response.body(), pdfsDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
				logger.info("Downloaded: " + fileName);
			}
			if (response.statusCode() == 302) {
				request = HttpRequest.newBuilder()
						.uri(URI.create(response.headers().firstValue("location")
								.orElseThrow(() -> new IOException("Missing location header in 302 response"))))
						.build();
				response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
				if (response.statusCode() == 200) {
					Files.copy(response.body(), pdfsDir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
					logger.info("Downloaded: " + fileName);
				} else {
					logger.error("Failed to download " + fileName + ": HTTP " + response.statusCode());
				}
			} else {
				logger.error("Failed to download " + fileName + ": HTTP " + response.statusCode());

			}
		} catch (Exception e) {
			logger.error("Failed to download " + fileName + ": ", e);
		}
	}
}
