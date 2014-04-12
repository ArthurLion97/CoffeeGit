package tk.circuitcoder.lab.CoffeeGit;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.errors.NotSupportedException;
import org.eclipse.jgit.errors.TransportException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.*;
import org.eclipse.jgit.transport.*;

public class Repo {
	private Repository repo;
	private Git git;
	private RepoStatus status;
	private String statusLine;
	private String errno;
	//TODO: error message dequeue
	
	private LinkedList<RepoActionListener> alList;
	
	public class RepoEvent {
		private Repo _r;
		private RepoStatus _rs;
		private String _sl;
		private String _e;
		private RepoAction _ra;
		private Object _re;
		
		private RepoEvent(Repo r,RepoStatus rs,String sl,String e,RepoAction ra,Object result) {
			_r=r;
			_rs=rs;
			_sl=sl;
			_e=e;
			_ra=ra;
			_re=result;
		}
		
		public Repo getRepo() {
			return _r;
		}
		
		public RepoStatus getRepoStatus() {
			return _rs;
		}
		
		public String getStatusLine() {
			return _sl;
		}
		
		public String getErrno() {
			return _e;
		}
		
		public RepoAction getAction() {
			return _ra;
		}
		
		public Object getResult() {
			return _re;
		}
	}
	
	public Repo(String dir) throws IOException {
		RepositoryBuilder rb=CoffeeGit.gerRepositoryBuilder();
		File fileDir=new File(dir);
		repo=rb.setGitDir(fileDir)
				.readEnvironment()
				.findGitDir()
				.build();
		
		String[] gitDirList=fileDir.list();
		boolean initFlag=false;
		for(int i=0;(!initFlag)&&i<gitDirList.length;i++)  if(gitDirList[i].equals(".git")) initFlag=true;
		
		if(!initFlag) status=RepoStatus.UNINITED;
		if(repo.getRef("refs/HEAD")==null) status=RepoStatus.EMPTY;
		else status=RepoStatus.FINE;
		
		alList=new LinkedList<RepoActionListener>();
	}
	
	public RepoStatus getStatus() {
		return status;
	}
	
	public String getStatusLine() {
		return statusLine;
	}
	
	public String getErrno() {
		return errno;
	}
	
	public Map<String,Ref> getRefMap() {
		return repo.getAllRefs();
	}
	
	public Map<String,Ref> getTagMap() {
		return repo.getTags();
	}
	
	public Map<String,Ref> getRemoteRefMap(String remote) throws NotSupportedException, TransportException, URISyntaxException {
		Transport t=Transport.open(repo,remote);
		FetchConnection fc=t.openFetch();
		return fc.getRefsMap();
	}
	
	public Map<String,Ref> getRemoteBranches(String remote) throws NotSupportedException, TransportException, URISyntaxException {
		Transport t=Transport.open(repo,remote);
		FetchConnection fc=t.openFetch();
		return fc.getRefsMap();
	}
	
	public DirCache doAdd(Collection<String> patterns) throws NoFilepatternException, GitAPIException  {
		AddCommand ac=git.add();
		for(String p:patterns) {
			ac.addFilepattern(p);
		}
		return ac.call();
	}
	
	public RepoStatus AddCommand(Collection<String> patterns) {
		DirCache dc;
		try {
			dc=doAdd(patterns);
		} catch (GitAPIException e) {
			status=RepoStatus.UNKNOW;
			statusLine="An error has occurred while adding resources. Click for more detail.";
			errno="Add Error, Error stack:\n"+e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		
		status=RepoStatus.FINE;
		RevCommit c;
		try {
			c = new RevWalk(repo).parseCommit(repo.getRef("HEAD").getObjectId());
		} catch (IOException e) {
			status=RepoStatus.UNKNOW;
			statusLine="An error has occurred while adding resources. Click for more detail.";
			errno="Add Error, Error stack:\n"+e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		statusLine=c.getId().getName()+"    "+c.getShortMessage();
		errno="Add Completed.\nFile Count: "+dc.getEntryCount();
		//TODO: file tree
		notifyActionListeners(RepoAction.ADD,null);
		return status;
	}
	
	public RevCommit doCommit(PersonIdent author,PersonIdent committer,String message) throws NoHeadException, NoMessageException, UnmergedPathsException, ConcurrentRefUpdateException, WrongRepositoryStateException, GitAPIException {
		return git.commit().setAuthor(author).setCommitter(committer).setMessage(message).call();
	}
	
	public RepoStatus commit(PersonIdent author,PersonIdent committer,String message) {
		RevCommit c;
		try {
			c=doCommit(author, committer, message);
		} catch (GitAPIException e) {
			status=RepoStatus.UNKNOW;
			statusLine="An error has occurred while committing. Click for more detail.";
			errno="Commit Error, Error stack:\n"+e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		statusLine=c.getId().getName()+"    "+c.getShortMessage();
		errno="Commit Completed.\nNew Head: "+c.getId().getName()+"    "+c.getShortMessage();
		
		notifyActionListeners(RepoAction.COMMIT,null);
		return (status=RepoStatus.FINE);
	}
	
	public MergeResult doMerge(String ref) throws GitAPIException, IOException {
		return git.merge().include(repo.getRef(ref)).call();
	}
	
	public RepoStatus merge(String ref) {
		MergeResult mr;
		try {
			mr = doMerge(ref);
		} catch (GitAPIException | IOException e) {
			status=RepoStatus.UNKNOW;
			errno=e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		RepoStatus rs=processMergeResult(mr);
		notifyActionListeners(RepoAction.MERGE,mr);
		return rs;
	}
	
	public FetchResult doFetch(String remote,List<RefSpec> specs,ProgressMonitor pm) throws InvalidRemoteException, org.eclipse.jgit.api.errors.TransportException, GitAPIException {
		if(pm==null) return git.fetch().setRemote(remote).setRefSpecs(specs).call();
		else return git.fetch().setRemote(remote).setRefSpecs(specs).setProgressMonitor(pm).call();
	}
	
	public RepoStatus fetch(String remote,List<RefSpec> specs,ProgressMonitor pm) {
		status=RepoStatus.FETCHING;
		FetchResult fr;
		try {
			fr = doFetch(remote, specs, pm);
		} catch (GitAPIException e) {
			status=RepoStatus.UNKNOW;
			errno=e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		status=RepoStatus.FETCHED;
		errno=fr.getMessages();
		notifyActionListeners(RepoAction.FETCH,fr);
		return status;
	}
	
	public PullResult doPull(String remote,String branch,ProgressMonitor pm,boolean useRebase)
			throws WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException, RefNotFoundException, NoHeadException, org.eclipse.jgit.api.errors.TransportException, GitAPIException{
		PullCommand pc=git.pull().setRemote(remote).setRemoteBranchName(branch);
		if(pm!=null) pc.setProgressMonitor(pm);
		if(useRebase) pc.setRebase(true);
		return pc.call();
	}
	
	public RepoStatus pull(String remote,String branch,ProgressMonitor pm,boolean useRebase) {
		status=RepoStatus.PULLING;
		PullResult pr;
		try {
			pr=doPull(remote, branch, pm,useRebase);
		} catch (GitAPIException e) {
			status=RepoStatus.UNKNOW;
			errno=e.getLocalizedMessage();
			e.printStackTrace();
			return status;
		}
		RepoStatus rs;
		if(useRebase) rs=processRebaseResult(pr.getRebaseResult());
		else rs=processMergeResult(pr.getMergeResult());
		
		notifyActionListeners(RepoAction.PULL,pr);
		return rs;
	}
	
	public Iterable<PushResult> doPush(String remote,List<RefSpec> specs,ProgressMonitor pm) throws InvalidRemoteException, org.eclipse.jgit.api.errors.TransportException, GitAPIException  {
		if(pm==null) return git.push().setRemote(remote).setRefSpecs(specs).call();
		else return git.push().setRemote(remote).setRefSpecs(specs).setProgressMonitor(pm).call();
	}
	
	public void push(String remote,List<RefSpec> specs,ProgressMonitor pm) {
		PushResult pr=null;
		
		notifyActionListeners(RepoAction.PUSH,pr);
	}
	
	private RepoStatus processMergeResult(MergeResult mr) {
		if(mr.getMergeStatus().isSuccessful()) {
			status=RepoStatus.MERGED;
			try {
				RevWalk rw=new RevWalk(repo);
				RevCommit rc=rw.parseCommit(mr.getNewHead());
				if(CoffeeGit.onDebug()) System.out.println("Merge Completed.\nNew Head: "+rc.getId().getName()+"    "+rc.getShortMessage());
				statusLine=rc.getId().getName()+"    "+rc.getShortMessage();
				errno="Merge Completed.\nNew Head: "+rc.getId().getName()+"    "+rc.getShortMessage();
			} catch(Exception e) {
				status=RepoStatus.UNKNOW;
			}
			return status;
		}
		else if(mr.getMergeStatus()==MergeStatus.CONFLICTING||
				mr.getMergeStatus()==MergeStatus.CHECKOUT_CONFLICT) {
			Map<String,int[][]> c=mr.getConflicts();
			//From javadoc
			StringBuilder confMsg=new StringBuilder("###Conflicts Detail###\n");
			for (String path : c.keySet()) {
				int[][] curr = c.get(path);
				System.out.println("Conflicts in file " + path);
				for (int i = 0; i < curr.length; ++i) {
					confMsg.append("  Conflict #").append(i).append('\n');
					for (int j = 0; j < (curr[i].length) - 1; ++j) {
						if (curr[i][j] >= 0)
							confMsg.append("    Chunk for ")
									.append(mr.getMergedCommits()[j])
									.append(" starts on line #")
			 						.append(curr[i][j])
			 						.append('\n');
					}
				}
			}
			if(CoffeeGit.onDebug()) System.out.print(confMsg);
			status=RepoStatus.CONFLICT;
			statusLine="Total: "+c.size()+" Files";
			errno=confMsg.toString();
			return status;
		} else {
			status=RepoStatus.UNKNOW;
			statusLine=mr.getMergeStatus().toString();
			errno="An Unexcepted Merge Error Has Occured";
			return RepoStatus.UNKNOW;
		}
	}
	
	@Deprecated
	private RepoStatus processRebaseResult(RebaseResult rr) {
		//NOT YET SUPPORTED
		return status;
	}
	
	public void addActionListener(RepoActionListener listener) {
		alList.addLast(listener);
	}
	
	public boolean RemoveActionListener(RepoActionListener listener) {
		return alList.remove(listener);
	}
	
	private void notifyActionListeners(RepoAction a,Object r) {
		RepoEvent re=new RepoEvent(this,status,statusLine,errno,a,r);
		Iterator<RepoActionListener> i=alList.iterator();
		switch(a) {
		case ADD:
			while(i.hasNext()) i.next().onAdd(re); break;
		case CHECKOUT:
			while(i.hasNext()) i.next().onCheckout(re); break;
		case COMMIT:
			while(i.hasNext()) i.next().onCommit(re); break;
		case FETCH:
			while(i.hasNext()) i.next().onFetch(re); break;
		case PULL:
			while(i.hasNext()) i.next().onPull(re); break;
		case PUSH:
			while(i.hasNext()) i.next().onPush(re); break;
		case RESET:
			while(i.hasNext()) i.next().onReset(re); break;
		case REBASE:
			while(i.hasNext()) i.next().onRebase(re); break;
		case MERGE:
			while(i.hasNext()) i.next().onMerge(re); break;
		case RM:
			while(i.hasNext()) i.next().onRm(re); break;
		}
	}
}
