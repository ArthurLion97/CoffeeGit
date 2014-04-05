package tk.circuitcoder.lab.CoffeeGit;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

@SuppressWarnings(value = {"unused"})
public class RepoRow extends JPanel {
	private static final long serialVersionUID = -1L;
	
	private static class RRProgressBar extends JProgressBar {
		private static final long serialVersionUID = -1L;
		RepoRow _parent;
		public RRProgressBar(RepoRow parent) {
			super();
			_parent=parent;
		}
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);
		}
	}
	
	private Repo _repo;
	private Font titleFont;
	private Font statusFont;
	private Font statusLineFont;
	private FontMetrics titleFM;
	private FontMetrics statusFM;
	private FontMetrics SLFM;
	private RRProgressBar pb;
	
	public RepoRow(Repo repository) {
		_repo=repository;
		
		titleFont=new Font(null,Font.BOLD,36);
		statusFont=new Font(null,Font.ITALIC,32);
		statusLineFont=new Font(null,Font.PLAIN,18);
		
		titleFM=getFontMetrics(titleFont);
		statusFM=getFontMetrics(statusFont);
		SLFM=getFontMetrics(statusLineFont);
		
		pb=new RRProgressBar(this);
	}
}
