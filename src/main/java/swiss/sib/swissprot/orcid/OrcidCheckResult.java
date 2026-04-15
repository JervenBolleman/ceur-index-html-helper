package swiss.sib.swissprot.orcid;

public record OrcidCheckResult(Status status, String name) {
	
	public OrcidCheckResult(Status status) {
		this(status, null);
	}

	public static enum Status{
		FAIL,
		OK,
	}

	public boolean isOk() {

		return status == Status.OK;
	}
}
