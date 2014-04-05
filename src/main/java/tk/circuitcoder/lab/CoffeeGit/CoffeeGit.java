package tk.circuitcoder.lab.CoffeeGit;

import org.eclipse.jgit.lib.RepositoryBuilder;

public class CoffeeGit {
	private static boolean debug=true;
	private static RepositoryBuilder rb;
	
	public static void main(String[] args) {
		System.out.println("WELL THIS IS WIP, then what are you doing here?");
	}
	
	public static boolean onDebug() {
		return debug;
	}
	
	public static RepositoryBuilder gerRepositoryBuilder() {
		return rb;
	}
}
