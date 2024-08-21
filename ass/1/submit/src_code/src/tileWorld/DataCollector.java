package tileWorld;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum DataCollector {
	INSTANCE;
	
	private String outputFile;
	private List<String> keys = new ArrayList<>();
	private Map<String, List<String>> data = new HashMap<>();
	
	private List<String> globalKeys = new ArrayList<>();
	private List<String> globalVals = new ArrayList<>();
	
	public void init(String outputFile) {
		setOutputFile(outputFile);
		keys = new ArrayList<>();
		data = new HashMap<>();
	}
	
	public void outputData() {
		File file = new File(this.outputFile);
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try(BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
			String globalFieldName = String.join(",", globalKeys);
			String globalFieldVal = String.join(",", globalVals);
			bw.write(globalFieldName + "\n");
			bw.write(globalFieldVal + "\n");

			bw.write("\n");
			String fieldName = String.join(",", keys);
			bw.write(fieldName + "\n");
			if (!keys.isEmpty()) {
				int length = data.get(keys.get(0)).size();
				for (int i = 0; i < length; i++) {
					for (int j = 0; j < keys.size(); j++) {
						String val = data.get(keys.get(j)).get(i);
						if (j == keys.size()-1) {
							bw.write(val + "\n");
						} else {
							bw.write(val + ",");
						}
					}
				}
			}
			System.out.println("The record was saved at " + file.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void setOutputFile(String outputFile) {
		this.outputFile = outputFile.endsWith(".csv") ? outputFile : outputFile+".csv";
	}
	
	public String getOutputFile() {
		return this.outputFile;
	}
	
	public Map<String, List<String>> getData() {
		return this.data;
	}
	
	public <T> void insertData(String key, T val) {
		if (!keys.contains(key)) {
			keys.add(key);
		}
		List<String> fields = data.getOrDefault(key, new ArrayList<>());
		fields.add(String.valueOf(val));
		data.put(key, fields);
	}
	
	public <T> void insertGlobalData(String key, T val) {
		globalKeys.add(key);
		globalVals.add(String.valueOf(val));
	}
}
