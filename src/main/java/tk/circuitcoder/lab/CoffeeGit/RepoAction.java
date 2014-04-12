package tk.circuitcoder.lab.CoffeeGit;

import org.eclipse.jgit.api.CheckoutResult;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.transport.FetchResult;
import org.eclipse.jgit.transport.PushResult;

public enum RepoAction {
	ADD(null),
	RM(null),
	COMMIT(null),
	CHECKOUT(CheckoutResult.class),
	FETCH(FetchResult.class),
	PULL(PullResult.class),
	PUSH(PushResult.class),
	RESET(null),
	MERGE(MergeResult.class),
	REBASE(RebaseResult.class);
	
	Class rc;
	
	private RepoAction(Class resultClass) {
		rc=resultClass;
	}
}