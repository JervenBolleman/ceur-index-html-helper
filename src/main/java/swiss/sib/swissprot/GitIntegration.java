package swiss.sib.swissprot;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoFilepatternException;

public class GitIntegration {
	private static final String GITHUB_WORKFLOW_PUBLISH = "/publish_github_pages.yaml";
	private static final String FORGEJO_WORKFLOW_PUBLISH = "/publish_forgejo_pages.yaml";
	private static final String GITLAB_WORKFLOW_PUBLISH = "/publish_gitlab_pages.yaml";

	private record PublishWorkflow(String resourceName, String path, String actionFileNameInRepo) {

	}

	private static final List<PublishWorkflow> PUBLISH_WORKFLOWS = List.of(
			new PublishWorkflow(GITHUB_WORKFLOW_PUBLISH, "./.github/workflows/", "publish_pages.yml"),
			new PublishWorkflow(FORGEJO_WORKFLOW_PUBLISH, "./.forgejo/workflows/", "publish_pages.yml"),
			new PublishWorkflow(GITLAB_WORKFLOW_PUBLISH, ".", ".gitlab-ci.yml"));

	public static void setupGit(File outputDir) throws IOException, IllegalStateException, GitAPIException {
		Path outputPath = outputDir.toPath();
		Path gitPath = outputPath.resolve(".git");
		try (Git git = openGit(outputDir, gitPath))
		{
			for (PublishWorkflow workflow : PUBLISH_WORKFLOWS) {
				Path workflowPath = outputPath.resolve(workflow.path);
				if (!Files.exists(workflowPath)) {
					Files.createDirectories(workflowPath);
					Path workFlowFile = workflowPath.resolve(workflow.actionFileNameInRepo);
					try (OutputStream nos = Files
							.newOutputStream(workFlowFile)) {
						GitIntegration.class.getResourceAsStream(workflow.resourceName).transferTo(nos);
					}
					addFile(git, workFlowFile);
				}
			}
		}
	}

	private static void addFile(Git git, Path workFlowFile) throws GitAPIException, NoFilepatternException {
		AddCommand addCommand = git.add();
		addCommand.addFilepattern(workFlowFile.toString());
		addCommand.call();
	}

	private static Git openGit(File outputDir, Path gitPath) throws GitAPIException, IOException {
		if (!Files.exists(gitPath)) {
			return Git.init().setDirectory(outputDir).call();
		} else {
			return Git.open(outputDir);
		}
	}
}
