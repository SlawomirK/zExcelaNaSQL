package wczytanie_z_xls;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import jxl.Cell;
import jxl.Sheet;
import jxl.read.biff.BiffException;
import jxl.Workbook;

public class Main {
	static Set<Preparat> preparaty = new HashSet<>();
	static String kolumnaNazwa;
	static String kolumnaNaCo;
	static Set<String> kolumnaSubCzynna;

	public static void main(String[] args) {
		try {
			wczytajPreparatyZ_XLS();
		} catch (BiffException | IOException e) {
			System.out
					.println("Blad wczytania z pliku excela" + e.getMessage());
			e.printStackTrace();
		}
		String[] nazwyKolumnSkladnik = { "IdSkladnik", "NazwaChemiczna" };
		String[] nazwyKolumnPreparat = { "IdPreparat", "nazwa_handlowa" };
		String[] nazwyKolumnRoslina = { "IdRoslina", "Nazwa" };
		// String[] nazwyKolumnDzialkaRolna = { "nr_ewidencyjny",
		// "powierzchia_ha" };
		String[] nazwyKolumnPatogen = { "id_patogenu", "nazwa_patogenu" };
		// String[] kolumnyHistoria_zabiegow = { "IdZabiegu", "data_wykonania",
		// "Dawka", "idPreparat", "id_uprawy" };
		String[] kolumnyPreparat_zwalcza = { "IdPreparat", "id_patogenu" };
		String[] kolSklad = { "idSkladnik", "idPreparat" };
		String[] kolCelZabiegu = { "idZabiegu", "id_patogenu" };

		try {

			przeksztalcNaPlikSQL("Skladnik", nazwyKolumnSkladnik);
			przeksztalcNaPlikSQL("Preparat", nazwyKolumnPreparat);
			przeksztalcNaPlikSQL("Roslina", nazwyKolumnRoslina);
			przeksztalcNaPlikSQL("Patogen", nazwyKolumnPatogen);
			// przeksztalcNaPlikSQL("Preparat_zwalcza",
			// kolumnyPreparat_zwalcza);
			przeksztalcNaPlikSQL("Sklad", kolSklad);
			// przeksztalcNaPlikSQL("CelZabiegu", kolCelZabiegu);
			// zakomentowane tabele bêd¹ uzupe³niane za pomoc¹ procedury
		} catch (IOException e) {
			System.out.println("B³¹d zapisu do pliku " + e.getMessage());
			e.printStackTrace();
		}
	}

	static StringBuilder suma = new StringBuilder();
	static StringBuilder tymczas = new StringBuilder();

	private static void przeksztalcNaPlikSQL(String nazwaTabeli,
			String[] nazwaKolumn) throws IOException {
		File file = new File("Preparaty.sql");
		if (!file.exists())
			file.createNewFile();
		FileWriter fw = new FileWriter(file.getAbsolutePath());
		BufferedWriter bw = new BufferedWriter(fw);
		StringBuilder polecenieWstawienia = new StringBuilder("INSERT INTO "
				+ nazwaTabeli + " (");
		StringBuilder temp = new StringBuilder();
		for (int i = 0; i < nazwaKolumn.length; i++) {
			if (i < nazwaKolumn.length - 1)
				temp.append(nazwaKolumn[i].concat(", "));
			else
				temp.append(new StringBuilder(nazwaKolumn[i].concat(")")));
		}
		polecenieWstawienia.append(temp.append(" VALUES ("));
		Set<String> wstawianie = null;
		suma.append("\n---------------TABELA " + nazwaTabeli.toUpperCase()
				+ "----------------------------------\n");
		switch (nazwaTabeli) {
		case "Skladnik":
			wstawianie = skladniki(preparaty);
			daneDoWstawienia(wstawianie, nazwaTabeli, bw, polecenieWstawienia);
			break;
		case "Preparat":
			wstawianie = nazwyHandlowe(preparaty);
			daneDoWstawienia(wstawianie, nazwaTabeli, bw, polecenieWstawienia);
			break;
		case "Roslina":
			wstawianie = rolinyUprawne();
			daneDoWstawienia(wstawianie, nazwaTabeli, bw, polecenieWstawienia);
			break;
		case "Dzialka_rolna":
			wstawianie = dzialkiRolne();
			daneDoWstawienia(wstawianie, nazwaTabeli, bw, polecenieWstawienia);
			break;
		case "Patogen":
			wstawianie = patogeny();
			daneDoWstawienia(wstawianie, nazwaTabeli, bw, polecenieWstawienia);
			break;
		case "Historia_zabiegow":
			wstawianie = hisoria_zabiegow();
			break;
		case "Preparat_zwalcza":
			stworzTab(preparat_zwalcza(), nazwaTabeli, bw, polecenieWstawienia);
			break;
		case "Sklad":
			stworzTab(sklad(), nazwaTabeli, bw, polecenieWstawienia);
			break;
		default:
			System.out.println("Nieprzewidziany b³¹d!!");
		}
		polecenieWstawienia.setLength(0);
		bw.newLine();
		bw.write(suma.toString());
		bw.close();
	}

	private static HashMap<Integer, Integer> sklad() {
		HashMap<Integer, Integer> sklad = new HashMap<>();
		int idSkladnika = 1;
		for (String skladnik : skladniki(preparaty)) {
			int idPreparat = 1;
			for (Preparat p : preparaty) {
				for (String s : p.getSubstancjaAktywna())
					if (s.contains(skladnik)) {
						sklad.put(idSkladnika, idPreparat);
						break;
					}
				idPreparat++;
			}
			idSkladnika++;
		}
		return sklad;
	}

	private static void stworzTab(HashMap<Integer, Integer> preparat_zwalcza,
			String nazwaTabeli, BufferedWriter bw,
			StringBuilder polecenieWstawienia) {
		preparat_zwalcza.forEach((k, v) -> {
			tymczas.append(polecenieWstawienia);
			tymczas.append(k + ", ");
			tymczas.append("'" + v + "');");
			suma.append(tymczas + "\n");
			tymczas.delete(0, tymczas.length());
		});
		System.out.println("NazwaTabeli" + nazwaTabeli + "tabele" + suma);
	}

	private static HashMap<Integer, Integer> preparat_zwalcza() {
		// przyporzadkowanie id preparatu do id patogenu<idPreparatu,idPatogenu>
		HashMap<Integer, Integer> coZwalczaCo = new HashMap<>();
		int pozycjaPreparatu = 1;
		for (Preparat p : preparaty) {
			int pozycjaPatogenu = 1;
			for (String s : patogeny()) {
				if (p.getSubstancjaAktywna().contains(s)) {
					coZwalczaCo.put(pozycjaPreparatu, pozycjaPatogenu);
					break;
				}
				pozycjaPatogenu++;
			}
			pozycjaPreparatu++;
		}
		return coZwalczaCo;
	}

	private static Set<String> hisoria_zabiegow() {
		// TODO Auto-generated method stub
		return null;
	}

	private static Set<String> patogeny() {
		// podstawowe grupami
		Set<String> patogeny = new HashSet<>();
		String[] tablicaPatogenow = { "Chwastobójczy", "jednoliscienne",
				"dwuliœcienne", "Fungicyd", "Insektycyd", "przedziorki",
				"Regulator wzrostu" };
		Collections.addAll(patogeny, tablicaPatogenow);
		return patogeny;
	}

	private static Set<String> dzialkiRolne() {
		Set<String> dzialki = new HashSet<>();
		String[] tablicaDzialek = { "879/2", "1005", "1007", "733/4", "697/2",
				"698/2", "694" };
		Collections.addAll(dzialki, tablicaDzialek);
		return dzialki;
	}

	private static void daneDoWstawienia(Set<String> wstawianie,
			String nazwaTabeli, BufferedWriter bw,
			StringBuilder polecenieWstawienia) throws IOException {
		int id = 1;
		for (String w : wstawianie) {
			tymczas.append(polecenieWstawienia);
			tymczas.append(id++ + ", ");
			tymczas.append("'" + w + "');");
			suma.append(tymczas + "\n");
			tymczas.delete(0, tymczas.length());
		}
	}

	private static Set<String> rolinyUprawne() {
		// zwraca roœliny uprawne, póki co tylko grupy roslin
		Set<String> rosliny = new HashSet<>();
		String[] tablicaRoslin = { "rzepak", "Zbo¿a ozime", "Zbo¿a jare",
				"Jab³onie", "porzeczki", "ziemniaki", "truskawki", "kukurydza" };
		Collections.addAll(rosliny, tablicaRoslin);
		return rosliny;
	}

	private static Set<String> nazwyHandlowe(Set<Preparat> preparaty2) {
		// zwraca nazwe handlow¹
		Set<String> nazwyHandl = new HashSet<>();
		for (Preparat p : preparaty2) {
			nazwyHandl.add(p.getNazwa());
		}
		return nazwyHandl;
	}

	private static Set<String> skladniki(Set<Preparat> preparaty2) {
		// wyci¹gn¹æ czyste nazwy, bez dawek, same nazwy handlowe
		Set<String> temp = new HashSet<>();
		for (Preparat p : preparaty2) {// po liscie preparatow
			for (String s : p.getSubstancjaAktywna()) {// po liœcie sk³adników
														// jednego preparatu
				if (!s.isEmpty())
					temp.add(s.substring(0, s.indexOf(" - ") + 1).trim());
			}
		}
		return temp;
	}

	private static void wczytajPreparatyZ_XLS() throws BiffException,
			IOException {
		Workbook skoroszyt = Workbook.getWorkbook(new File(
				"rejestr_sor20.03.2017.xls"));
		Sheet arkusz = skoroszyt.getSheet(0);
		Cell komorka = arkusz.getCell(0, 0);
		for (int wiersz = 1; wiersz < arkusz.getRows(); wiersz++) {
			for (int kolumna = 1; kolumna < arkusz.getColumns(); kolumna++) {
				komorka = arkusz.getCell(kolumna, wiersz);
				if (kolumna == 2)
					kolumnaNazwa = komorka.getContents();
				if (kolumna == 3)
					kolumnaNaCo = komorka.getContents();
				if (kolumna == 6)
					kolumnaSubCzynna = wydzielSubAkt(komorka.getContents());
			}
			preparaty.add(new Preparat(kolumnaNazwa, kolumnaSubCzynna,
					kolumnaNaCo));
		}
		skoroszyt.close();
		System.out.println("Odczyt zakoñczony sukcesem.");
	}

	private static Set<String> wydzielSubAkt(String contents) {
		String[] temp = contents.split("g,");
		Set<String> skladniki = new HashSet<>();
		for (String t : temp)
			if (!t.isEmpty())
				skladniki.add(t);
		return skladniki;
	}
}
