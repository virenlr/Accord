package com.lukehere.app.accord;

public class Attendee {
    private int mRegistrationNumber;

    private String mName;

    private int mAge;

    private String mGender;

    private String mStream;

    private String mAttendeeType;

    private String mInstitution;

    private String mDesignation;

    private String mEmailAddress;

    private String mPhoneNumber;

    private int mPriority;

    private int mBlackList;

    Attendee() {
    }

    public int getRegistrationNumber() {
        return mRegistrationNumber;
    }

    public void setRegistrationNumber(int mRegistrationNumber) {
        this.mRegistrationNumber = mRegistrationNumber;
    }

    public String getName() {
        return mName;
    }

    public void setName(String mName) {
        this.mName = mName;
    }

    public int getAge() {
        return mAge;
    }

    public void setAge(int mAge) {
        this.mAge = mAge;
    }

    public String getGender() {
        return mGender;
    }

    public void setGender(String mGender) {
        this.mGender = mGender;
    }

    public String getStream() {
        return mStream;
    }

    public void setStream(String mStream) {
        this.mStream = mStream;
    }

    public String getAttendeeType() {
        return mAttendeeType;
    }

    public void setAttendeeType(String mAttendeeType) {
        this.mAttendeeType = mAttendeeType;
    }

    public String getInstitution() {
        return mInstitution;
    }

    public void setInstitution(String mInstitution) {
        this.mInstitution = mInstitution;
    }

    public String getDesignation() {
        return mDesignation;
    }

    public void setDesignation(String mDesignation) {
        this.mDesignation = mDesignation;
    }

    public String getEmailAddress() {
        return mEmailAddress;
    }

    public void setEmailAddress(String mEmailAddress) {
        this.mEmailAddress = mEmailAddress;
    }

    public String getPhoneNumber() {
        return mPhoneNumber;
    }

    public void setPhoneNumber(String mPhoneNumber) {
        this.mPhoneNumber = mPhoneNumber;
    }

    public int getPriority() {
        return mPriority;
    }

    public void setPriority(int mPriority) {
        this.mPriority = mPriority;
    }

    public int getBlackList() {
        return mBlackList;
    }

    public void setBlackList(int mBlackList) {
        this.mBlackList = mBlackList;
    }
}
