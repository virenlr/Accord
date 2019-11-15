# Accord
This is Accord, the Android application that was used by the Student Council of CHRIST (Deemed to be University) to handle registrations and stall-wise feedback during Daksh, the annual Education Fair.

You can download and install the app from the Google Play Store by clicking [here](https://play.google.com/store/apps/details?id=com.lukehere.app.accord).

## Features

### The Main Screen
When you open Accord, you will be greeted with an array of cards, each representing a registered attendee. From this screen, you can either click on an attendee card to open up the Editor, or just scroll down the list to have a rough visual glance through the form. Pulling from the top of the screen will cause the cards to refresh themselves with the latest data from the server. Keeping in mind the constraints in network connectivity, Accord has been specially optimized to be able to run in low-network environments by transferring very little data, and by caching data where possible. The algorithm ensures that the data presented to the user is always up to date.

### The App Bar
The App Bar allows you to navigate within the application. Clicking on the first icon will open the Editor, a customized environment for registering attendees in a fast and dynamic manner. The second icon opens up the Statistics pane, the in-app solution to being able to analyze data on-the-go. The overflow menu contains the option of being able to export the entire attendee database to the SD card with just the click of a button.

### The Editor
The Editor is the heart of Accord. Here, you may input all the attendee information. At the top right of the page, you will notice three icons. The Search option allows you to manually confirm with the system as to whether an attendee entry exists or not. The Scan QR code option enables you to scan a QR code to be able to input the registration number without typing. The Update button updates all the information to the server.

### Priority
Within the Editor, an attendee can be marked with three levels of priority- low, medium, and high. Attendees marked with a high priority will be sent an SMS and an email after the event, those marked with medium priority will receive only an email, and those marked with low priority may not receive any follow up at all. The SMS and email portions must be handled manually.

### Feedback
The Feedback pane allows attendees to input their feedback on a standard set of questions, should they so wish to. If an attendee does not want to give any input, the Feedback switch can be simply turned off, and the attendee will still be given attendance at that stall without having given any feedback.

### Statistics
The Statistics feature of Accord allows you to look at a collated view of the actual data being generated at the event. Simple and quick to understand, Statistics enable Student Council members to keep track of the progress of their stall, and mentors to keep a record of the event as a whole.

### Export
As the name suggests, Export allows you to export the entire list of attendees to the internal storage of your mobile device with just a few clicks. From there, the Excel file can be opened directly or transferred to a computer. Using simple Excel tools, data can be sorted and filtered within the sheet. The priority lists can also be extracted with ease.

### Security
Security is a major concern while developing any mobile application. This is why Accord comes with a set of security features built-in, features that are not only powerful but also light and seamless. Users of Accord must authenticate themselves before being able to use the app, and the feedback uploaded to the server will be signed with the authentication information of the Student Council member who submitted it. This ensures total transparency in every transaction. Furthermore, feedback once submitted cannot be edited or modified in any way. Thus, the app ensures the integrity of data and fair-play in its usage.
