package wczytanie_z_xls;

import java.util.Set;

public class Preparat {
private String nazwa;
private Set<String> substancjaAktywna;
private String stosowanyPrzeciw;

public Preparat(String nazwa,Set<String> substancjaAktywna,String stosowanyPrzeciw){
	this.nazwa=nazwa;
	this.substancjaAktywna=substancjaAktywna;
	this.stosowanyPrzeciw=stosowanyPrzeciw;
}
public String getStosowanyPrzeciw() {
	return stosowanyPrzeciw;
}
public void setStosowanyPrzeciw(String stosowanyPrzeciw) {
	this.stosowanyPrzeciw = stosowanyPrzeciw;
}
public Set<String> getSubstancjaAktywna() {
	return substancjaAktywna;
}
public void setSubstancjaAktywna(Set<String> substancjaAktywna) {
	this.substancjaAktywna = substancjaAktywna;
}
public String getNazwa() {
	return nazwa;
}
public void setNazwa(String nazwa) {
	this.nazwa = nazwa;
}

}
