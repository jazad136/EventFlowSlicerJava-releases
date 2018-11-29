package edu.unl.cse.efs.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestDataRipper {
		
		public static class ManifestMap implements Map<String, Object>
		{
			private HashMap<String, Object> manifestMap;
			public ManifestMap()
			{
				manifestMap = new HashMap<String, Object>();
			}
			
			@Override
			public int size() {
				// TODO Auto-generated method stub
				return 0;
			}

			@Override
			public boolean isEmpty() {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean containsKey(Object key) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public boolean containsValue(Object value) {
				// TODO Auto-generated method stub
				return false;
			}

			@Override
			public Object get(Object key) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object put(String key, Object value) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Object remove(Object key) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public void putAll(Map<? extends String, ? extends Object> m) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void clear() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public Set<String> keySet() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Collection<Object> values() {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public Set<java.util.Map.Entry<String, Object>> entrySet() {
				// TODO Auto-generated method stub
				return null;
			}
			
		}
		public static class ManifestDataEntry implements Map.Entry<String, Object>
		{
			private final String key;
			private Object value;
			public ManifestDataEntry(String key, Object value)
			{
				this.key = key;
				this.value = value;
			}
			@Override
			public String getKey() {
				return key.toString();
			}

			@Override
			public String getValue() 
			{
				return value.toString();
			}

			public Object setValue(Object value) {
				Object oldValue = value;
				this.value = value;
				return oldValue.toString();
			}
			public String toString()
			{
				return "" + key + ": " + value;
			}
		}
		
		public static String sp(int numSpaces)
		{
			return " " + sp(numSpaces - 1);
		}
		public String toString()
		{
			String toReturn = "---Manifest Data Table Entries---";
			toReturn += sp(4);
			return toReturn;
			
		}
		public static String stringsOf(List<ManifestDataEntry> toConvert)
		{
			return Arrays.deepToString(toConvert.toArray());
		}
		
		public static List<ManifestDataEntry> getMainManifestAttributes(Manifest mf)
		{
			ArrayList<ManifestDataEntry> mfData = new ArrayList<ManifestDataEntry>();
			// load the information. 
			Attributes attributes = mf.getMainAttributes();
			for(Object a : attributes.keySet()) 
				mfData.add(new ManifestDataEntry(a.toString(), attributes.get(a)));
			return mfData;
		}
		
		
		public static List<ManifestDataEntry> getAllManifestData(Manifest mf)
		{
			ArrayList<ManifestDataEntry> mfData = new ArrayList<ManifestDataEntry>();
			// load the information. 
			
			Map<String, Attributes> theMap = mf.getEntries();
			
			mfData.addAll(getMainManifestAttributes(mf));
			
			for(String s : theMap.keySet()) 
				mfData.add(new ManifestDataEntry(s, theMap.get(s)));
			
			return mfData;
		}
	}