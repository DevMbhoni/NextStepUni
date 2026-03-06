package model;

public class Qualification {
    private int qualID;
    private String qualName;
    private String qualType;
    private String qualFaculty;

    public Qualification(int qualID,String qualName, String qualType, String qualFaculty) {
        this.qualID = qualID;
        this.qualName = qualName;
        this.qualType = qualType;
        this.qualFaculty = qualFaculty;
    }

    public Qualification(String qualName, String qualType, String qualFaculty) {
        this.qualName = qualName;
        this.qualType = qualType;
        this.qualFaculty = qualFaculty;
    }
    public int getQualID() {
        return qualID;
    }

    @Override
    public String toString() {
        return qualName + " (" + qualType + ")";
    }

    public String getQualName() {
        return qualName;
    }

    public String getQualType() {
        return qualType;
    }

    public String getQualFaculty() {
        return qualFaculty;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Qualification)) return false;
        Qualification that = (Qualification) o;
        return this.getQualID() == that.getQualID();
    }
}