package tk.circuitcoder.lab.CoffeeGit;

public enum RepoStatus {
	FINE(false,true,"Repository good to go"),
	UNINITED(false,true,"Repository not initialized"),
	EMPTY(false,true,"Empty repository"),
	
	FETCHING(false,false,"Fetching from remote..."),
	PUSHING(false,false,"Pulling from remote..."),
	PULLING(false,false,"Pushing to remote..."),
	
	FETCHED(false,true,"Fetch completed"),
	PUSHED(false,true,"Push completed"),
	MERGED(false,true,"Merge completed"),
	REBASED(false,true,"Rebase completed"),
	PULLED(false,true,"Pull completed"),
	
	CONFLICT(true,true,"An merge/pull conflict has occured. Click for more detail."),
	UNKNOW(true,true,"An unknow error has occured. Click for more detail.");
	
	private boolean e;
	private boolean i;
	private String dt;
	private RepoStatus(boolean error,boolean idle,String defaultText) {
		e=error;
		i=idle;
		dt=defaultText;
	}
	
	public boolean isError() {
		return e;
	}
	
	public boolean isIdle() {
		return i;
	}
	
	public String getDefaultString() {
		return dt;
	}
}