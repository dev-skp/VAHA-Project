// Project : Voice Activated Home Automation and Voice Commands is received from VAHA App by Sudhir

#define load1 2    // Connect relay1 (IN1) to pin 2
#define load2 3    // Connect relay2 (IN2) to pin 3
#define load3 4    // Connect relay3 (IN3) to pin 4
#define load4 5    // Connect relay4 (IN4) to pin 5
String voiceData;

void setup() {
  Serial.begin(9600);           // Set rate for communicating with phone
  pinMode(load1, OUTPUT);       // Set Load1 as an output
  pinMode(load2, OUTPUT);       // Set Load2 as an output
  pinMode(load3, OUTPUT);       // Set Load3 as an output
  pinMode(load4, OUTPUT);       // Set Load4 as an output
  digitalWrite(load1, HIGH);    // Switch off Load1 (Use NO Connection of Relay)
  digitalWrite(load2, HIGH);    // Switch off Load2
  digitalWrite(load3, HIGH);    // Switch off Load3
  digitalWrite(load4, HIGH);    // Switch off Load4

  Serial.println("\nVoice Activated Home Automation System Ready!");
}

void loop() {
  while (Serial.available()) {  // Check if there is an available byte to read
    delay(10);                 // Delay added to make things stable
    char c = Serial.read();    // Conduct a serial read
    if (c == '#') {            // Exit the loop when the # is detected after the word
      break;
    }
    voiceData += c;            // Shorthand for voiceData = voiceData + c
  }

  if (voiceData.length() > 0) {
    // Convert the received command from VAHA App to uppercase
    voiceData.toUpperCase.trim();

    // Print the received voice command to the Serial Monitor
    Serial.print("Received Command: ");
    Serial.println(voiceData);

    //**** Turn ON device based on Voice Data Received from VAHA App ****//

    // Voice Command to turn ON Relay 01
    if (voiceData == "TURN ON LIGHT" || voiceData == "LIGHT ON" || voiceData == "LIGHT" || voiceData == "LIGHT CHALU") {          
      digitalWrite(load1, LOW);                  // Relay 01 ON (IN1)
      Serial.println("Light turned ON");
    } 
    
    // Voice Command to turn ON Relay 02
    else if (voiceData == "TURN ON FAN" || voiceData == "FAN ON" || voiceData == "FAN" || voiceData == "PANKHA CHALU") {     
      digitalWrite(load2, LOW);                  // Relay 02 ON (IN2)
      Serial.println("Fan turned ON");
    }
    
    // Voice Command to turn ON Relay 03
    else if (voiceData == "TURN ON TV" || voiceData == "TV ON" || voiceData == "TV" || voiceData == "TV CHALU") {      
      digitalWrite(load3, LOW);                  // Relay 03 ON (IN3)
      Serial.println("TV turned ON");
    } 

    // Voice Command to turn ON Relay 04
    else if (voiceData == "TURN ON AC" || voiceData == "AC ON" || voiceData == "AC" || voiceData == "AC CHALU") {      
      digitalWrite(load4, LOW);                  // Relay 04 ON (IN4)
      Serial.println("AC turned ON");
    } 

    //**** Turn OFF device based on Voice Data Received from VAHA App ****//

    // Voice Command to turn OFF Relay 01
    else if (voiceData == "TURN OFF LIGHT" || voiceData == "LIGHT OFF" || voiceData == "LIGHT BAND") {    
      digitalWrite(load1, HIGH);                 // Relay 01 OFF (IN1)
      Serial.println("Light turned OFF");
    } 
    
    // Voice Command to turn OFF Relay 02
    else if (voiceData == "TURN OFF FAN" || voiceData == "FAN OFF" || voiceData == "FAN BAND") {    
      digitalWrite(load2, HIGH);                 // Relay 02 OFF (IN2)
      Serial.println("Fan turned OFF");
    } 
    
    // Voice Command to turn OFF Relay 03
    else if (voiceData == "TURN OFF TV" || voiceData == "TV OFF" || voiceData == "TV BAND") {     
      digitalWrite(load3, HIGH);                 // Relay 03 OFF (IN3)
      Serial.println("TV turned OFF");
    } 
    
    // Voice Command to turn OFF Relay 04
    else if (voiceData == "TURN OFF AC" || voiceData == "AC OFF" || voiceData == "AC BAND") {     
      digitalWrite(load4, HIGH);                 // Relay 04 OFF (IN4)
      Serial.println("AC turned OFF");
    } 

    //**** Additional Commands to Control All Devices Turn ON/OFF ****//

    // Voice Command to ON all Relays
    else if (voiceData == "TURN ON ALL DEVICES" || voiceData == "SAB CHALU") {  
      turnOnAll();                                 // All Relays ON Simultaneously
      Serial.println("All devices turned ON");
    } 
    
    // Voice Command to OFF all Relays
    else if (voiceData == "TURN OFF ALL DEVICES" || voiceData == "SAB BAND") { 
      turnOffAll();                                 // All Relays OFF Simultaneously
      Serial.println("All devices turned OFF");
    } 
    
    voiceData = "";  // Reset the variable after initiating
  }
}

//**** Defining Function for Controlling All Devices by ON/OFF Command ****//
// Function for turning OFF all relays
void turnOffAll() {  
  digitalWrite(load1, HIGH);
  digitalWrite(load2, HIGH);
  digitalWrite(load3, HIGH);
  digitalWrite(load4, HIGH);
}

// Function for turning ON all relays
void turnOnAll() {   
  digitalWrite(load1, LOW);
  digitalWrite(load2, LOW);
  digitalWrite(load3, LOW);
  digitalWrite(load4, LOW);
}