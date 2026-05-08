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
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
public class OxfordAbstractsDownloader implements Callable<Integer>{
	private static final Logger logger = LoggerFactory.getLogger(OxfordAbstractsDownloader.class); 

	private static final String API_URL = "https://graphql.oxfordabstracts.com/v1/graphql";

	@Option(names = "--download-dir-pdfs", description = "What will be the input directory containing a preface.pdf and directories for each section containing all the paper pdfs", required = true)
	Path pdfsDir;

	@Option(names = "--download-dir-agreements", description = "Where will you download the uploaded CEUR agreements to")
	Path agreementsDir;

	@Option(names = "--api-token", required=true)
	private String authToken = "YOUR_API_TOKEN";

	@Option(names = "--event-id", required=true)
	private String eventId = "YOUR_EVENT_ID";

	@Option(names = "--pdf-field-name", description = "The name of the field that contains the PDFs that you are interested in", required=true)
	private String targetField = "Upload PDF"; // Change to your field name

	

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
		// 1. Prepare the GraphQL Query
		String query = """
query GetSubmissionsForEvent {
  events_by_pk(id: $eventId) {
    id
    submissions {
      id
      decision {
        value
      }
      accepted_for {
        value
      }
      title {
        without_html
      }
      responses {
        value
        question {
          question_name
          data_type
          api_name
          archived
          id
        }
      }
    } 
  }
}""";

		String qr = query.replace("$eventId", eventId);
		logger.info(qr);
		String jsonPayload = mapper.writeValueAsString(
				java.util.Map.of("query", qr));
		logger.info(jsonPayload);
		return jsonPayload;
	}

	private JsonNode sendRequests(HttpClient client, ObjectMapper mapper, String jsonPayload)
			throws IOException, InterruptedException, JsonProcessingException, JsonMappingException {
		logger.info(">>>"+authToken+"<<<");
		HttpRequest request = HttpRequest.newBuilder(
				).uri(URI.create(API_URL))
				.header("Content-Type", "application/json")
				.header("x-api-key", authToken)
				.POST(BodyPublishers.ofString(jsonPayload)).build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		JsonNode root = mapper.readTree(response.body());
		logger.info(response.body());
		JsonNode submissions = root.path("data").path("event").path("submissions");
		return submissions;
	}

	private void processAndDownload(HttpClient client, JsonNode submissions) {
		for (JsonNode sub : submissions) {
			String subId = sub.path("id").asText();
			String status = sub.path("decision").path("value").asText("No_Decision");

			for (JsonNode resp : sub.path("responses")) {
				String questionTitle = resp.path("question").path("question_name").asText();
				logger.info("sub: {}, status:{}, question:{}", subId, status, questionTitle);
//				if (targetField.equals(questionTitle) && resp.has("file")) {
//					String fileUrl = resp.path("file").path("url").asText();
//					String originalName = resp.path("file").path("fileName").asText();
//					String extension = originalName.contains(".")
//							? originalName.substring(originalName.lastIndexOf("."))
//							: ".pdf";
//
//					String fileName = "submission_" + subId + extension;
//
//					downloadFile(client, fileUrl, fileName, status);
//				}
			}
		}
	}

	private static void downloadFile(HttpClient client, String url, String fileName, String status) {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
			HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

			Files.copy(response.body(), Paths.get(fileName), StandardCopyOption.REPLACE_EXISTING);
			System.out.println("Downloaded: " + fileName);
		} catch (Exception e) {
			System.err.println("Failed to download " + fileName + ": " + e.getMessage());
		}
	}
}
