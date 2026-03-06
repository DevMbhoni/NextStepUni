package model;

import java.time.LocalDate;

public class Student {
    private int studentID;
    private String stuLName;
    private String stuFName;
    private String proofOfReg;
    private String emailAddress;
    private String password;
    private LocalDate dateOfBirth;
    private boolean isVerified;
    private int universityID;
    private String universityname;

    public Student(int studentID, String stuLName, String stuFName, String proofOfReg, String emailAddress, String password, LocalDate dateOfBirth, boolean isVerified, int universityID, String universityname) {
        this.studentID = studentID;
        this.stuLName = stuLName;
        this.stuFName = stuFName;
        this.proofOfReg = proofOfReg;
        this.emailAddress = emailAddress;
        this.password = password;
        this.dateOfBirth = dateOfBirth;
        this.isVerified = isVerified;
        this.universityID = universityID;
        this.universityname = universityname;
    }

    public Student(int studentID, String stuFName, String stuLName, String emailAddress,
                   LocalDate dateOfBirth, String proofOfReg, String universityName) {
        this.studentID = studentID;
        this.stuFName = stuFName;
        this.stuLName = stuLName;
        this.emailAddress = emailAddress;
        this.dateOfBirth = dateOfBirth;
        this.proofOfReg = proofOfReg;
        this.universityname = universityName;
    }

    public String getPassword() {
        return password;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public int getStudentID() {
        return studentID;
    }

    public String getStuLName() {
        return stuLName;
    }

    public String getStuFName() {
        return stuFName;
    }

    public String getProofOfReg() {
        return proofOfReg;
    }

    public String getEmailAddress() {
        return emailAddress;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public int getUniversityID() {
        return universityID;
    }

    public String getUniversityname() {
        return universityname;
    }
}