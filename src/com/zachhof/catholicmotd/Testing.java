package com.zachhof.catholicmotd;

import java.io.IOException;

public class Testing {
	public static void main(String[] args) {
		System.out.println("Starting test...");
		int[] calRange = {0, 60};
//		int[] verseRange = {-60, 60};
		try {
			//calendar
			for (int i = calRange[0]; i < calRange[1]; ++i) {
				String date = Messager.getCurrentDateString("yyyy/MM/dd", i);
				Calendar cal = Calendar.getDailyCalendar(date);
				System.out.println(i + ": " + cal.displayFormat());
			}
			//verses
//			for (int i = verseRange[0]; i < verseRange[1]; ++i) {
//				String date = Messager.getCurrentDateString("yyyy/MM/dd", i);
//				System.out.println(i + ": " + Messager.getDailyVerse("RSVCE", date));
//			}
			//catechism
			System.out.println(Messager.getRandomCatechism(200));
		} catch (IOException ex) {
			ex.printStackTrace();
		}
		System.out.println("\nTESTS COMPLETE");
	}
}
