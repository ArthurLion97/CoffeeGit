package tk.circuitcoder.lab.CoffeeGit;

import tk.circuitcoder.lab.CoffeeGit.Repo.RepoEvent;


public interface RepoActionListener {
	public abstract void onAdd(RepoEvent e);
	public abstract void onRm(RepoEvent e);
	public abstract void onCommit(RepoEvent e);
	public abstract void onCheckout(RepoEvent e);
	public abstract void onFetch(RepoEvent e);
	public abstract void onPull(RepoEvent e);
	public abstract void onPush(RepoEvent e);
	public abstract void onReset(RepoEvent e);
	public abstract void onMerge(RepoEvent e);
	public abstract void onRebase(RepoEvent e);
}