package org.meresco.lucenejena;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Random;

public class MainMassiveURIenumerateProbeer {

	private static final int CACHE_SIZE = 1000 * 1000;
	private static final double N = 1000 * 1000 * 1000;
	private static final long INTERVAL = 100 * 1000;

	public static void main(String[] args) throws Exception {
		String name = "/home/erik/host/no-backup/lucene-jena-pruts";
		Path path = FileSystems.getDefault().getPath(name);
		UriEnumerate uris = new UriEnumerate(path, CACHE_SIZE);

		Random random = new Random();
		long t0 = System.currentTimeMillis();
		long t1 = System.currentTimeMillis();
		for (int i = 1; i < N; i++) {
			if (i % INTERVAL == 0) {
				long t = t1;
				t1 = System.currentTimeMillis();
				long ops = INTERVAL / (t1 - t);
				long ops_avg = i / (t1 - t0);
				System.out.println(i + "  speed: " + ops + "k/s  avg: " + ops_avg + "k/s");
				String uri = uris.get(random.nextInt(i / 10)); // random check
				assert uri.startsWith("http://");
			}
			uris.put("http://" + random.nextInt((int) (N / 10)));
			// String uri = uris.get(ord);
		}
		uris.close();
	}
}
