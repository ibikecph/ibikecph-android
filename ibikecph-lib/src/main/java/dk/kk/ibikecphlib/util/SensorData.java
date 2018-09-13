package dk.kk.ibikecphlib.util;

public class SensorData {
	private float[][] values;
	private int currentIndex = 0;
	private int countToCache;
	private int itemCount;
	private int numValid = 0;

	public SensorData(int countToCache, int itemCount) {
		this.countToCache = countToCache;
		this.itemCount = itemCount;
		this.values = new float[countToCache][itemCount];
	}

	public void put(float[] values) {
		if (values.length == itemCount) {
			for (int i = 0; i < itemCount; i++) {
				this.values[currentIndex][i] = values[i];
			}
			currentIndex = (currentIndex + 1) % countToCache;
			if (numValid < countToCache) {
				numValid++;
			}
		}
	}

	public float[] get() {
		float[] ret = new float[itemCount];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = 0f;
		}

		for (int i = 0; i < countToCache && i < numValid; i++) {
			for (int j = 0; j < itemCount; j++) {
				ret[j] += values[i][j];
			}
		}

		for (int i = 0; i < ret.length; i++) {
			ret[i] /= countToCache;
		}

		return ret;
	}

}
