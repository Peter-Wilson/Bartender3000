int ind1=0;  //inductive sensor States
int ind2=0;
int ind3=0;

int sw1=0;  // Drink Switch States
int sw2=0;
int sw3=0;
int sw4=0;

int ls1=0;  //Limit Switch States
int ls2=0;
int ls3=0;
int ls4=0;

int cupCheck=0;  //Cup Check

int motorMoveFor=0;  //Motor Moving States
int motorMoveRev=0;

int drinkReady=0;
int drinkFin=0;

int drink1=0;  //Drink Selection (Only 1 can be selected at a time)
int drink2=0;
int drink3=0;
int drink4=0;

const int ind1pin = 13;  //inductive pins 
const int ind2pin = 12;
const int ind3pin = 11;

const int switch1pin = 10;  //drink button pins
const int switch2pin = 9;  
const int switch3pin = 8;  
const int switch4pin = 7;  

const int arm1location = 6;  //arm locations
const int arm2location = 5;
const int arm3location = 4;
const int arm4location = 3;

const int rumPin = x;  //Liquids
const int vodkaPin = x;
const int whiskeyPin = x;
const int cokePin = x;
const int cranPin = x;
const int sourPin = x;
const int orangePin = x;

const int motorForPin = x;  //Motor Movement
const int motorRevPin = x;

void setup() {
 
    pinMode(ind1pin, INPUT);
    pinMode(ind2pin, INPUT);
    pinMode(ind3pin, INPUT);
    
    pinMode(switch1pin, INPUT);
    pinMode(switch2pin, INPUT);
    pinMode(switch3pin, INPUT);
    pinMode(switch4pin, INPUT);
    
    pinMode(arm1location, INPUT);
    pinMode(arm2location, INPUT);
    pinMode(arm3location, INPUT);
    pinMode(arm4location, INPUT);
    
    pinMode(rumPin, OUTPUT);
    pinMode(vodkaPin, OUTPUT);
    pinMode(whiskeyPin, OUTPUT);
    pinMode(cokePin, OUTPUT);
    pinMode(cranPin, OUTPUT);
    pinMode(sourPin, OUTPUT);
    pinMode(orangePin, OUTPUT);
    
    pinMode(motorForPin, OUTPUT);
    pinMode(motorRevPin, OUTPUT);
    Serial.begin(9600);
}


void loop(){

  //Updating Variable States
  ind1 = digitalRead(ind1pin);
  ind2 = digitalRead(ind2pin);
  ind3 = digitalRead(ind3pin);

  sw1 = digitalRead(switch1pin);
  sw2 = digitalRead(switch2pin);
  sw3 = digitalRead(switch3pin);
  sw4 = digitalRead(switch4pin);
    
  ls1 = digitalRead(arm1location);
  ls2 = digitalRead(arm2location);
  ls3 = digitalRead(arm3location);
  ls4 = digitalRead(arm4location);
  
  //Cup Check Code
  if (ind1 == 1 || ind2 == 1 || ind3 == 1) {

    Serial.println("Cup Checked");
    cupCheck=1;
  }
  else {
    cupCheck=0;
    Serial.println("Cup Not Found");
  }
  
  //Drink Selection Lockout
  if (sw1 == 1 && cupCheck == 1 && ls4 == 1) {
    Serial.println("Drink 1 Selected");  
    drink1=1;
    drink2=0;
    drink3=0;
    drink4=0;
    motorMoveFor=1; //Cup at location, drink selected, moving arm
 
 } 
    
   //Drink Selection Lockout
  if (sw2 == 1 && cupCheck == 1 && ls4 == 1) {
    Serial.println("Drink 2 Selected");  
    drink1=0;
    drink2=1;
    drink3=0;
    drink4=0;
    motorMoveFor=1; //Cup at location, drink selected, moving arm

  } 
   
   //Drink Selection Lockout
  if (sw3 == 1 && cupCheck == 1 && ls4 ==1) {
    Serial.println("Drink 3 Selected");  
    drink1=0;
    drink2=0;
    drink3=1;
    drink4=0;
    motorMoveFor=1; //Cup at location, drink selected, moving arm

  } 
   //Drink Selection Lockout
  if (sw4 == 1 && cupCheck == 1 && ls4 == 1) {
    Serial.println("Drink 4 Selected");  
    drink1=0;
    drink2=0;
    drink3=0;
    drink4=1;
    motorMoveFor=1; //Cup at location, drink selected, moving arm
  } 
  
  //Move Arm
  if (motorMoveFor == 1) {
    motorMoveRev=0;
    //change pin state to high
  }
 if (motorMoveRev == 1) {
   motorMoveFor=0;
   //bring arm home
  }
 
 //Stops Arm When it is in Home Position
 if (motorMoveRev == 1 && ls4 ==1) {
   motorMoveRev=0;
   Serial.println("Arm At Home Position");
 } 
 
 //Reset Drink Finished
 if (ls4 == 1 && cupCheck == 0) {
   drinkFin=0;
 }
 
  //Stopping Arm at correct location
 //Location 1
  if (motorMoveFor == 1 && ls1 == 1) {
    
    Serial.println("Arm in Location 1, Ready To Pour Drink");
    motorMoveFor=0;
    drinkReady=1;
  }
  //Location 2
   if (motorMoveFor == 1 && ls2 == 1) {
    
    Serial.println("Arm in Location 2, Ready To Pour Drink");
    motorMoveFor=0;
    drinkReady=1;
  }
  //Location 3
  if (motorMoveFor == 1 && ls3 == 1) {
    
    Serial.println("Arm in Location 3, Ready To Pour Drink");
    motorMoveFor=0;
    drinkReady=1;
  }
 
  //Pouring Rum & Coke Drink 1
  if (drinkReady ==1 && drink1 == 1) {
    digitalWrite(rumPin, HIGH);
    delay(5000);
    //Pours Rum for 5 sec
    digitalWrite(rumPin, LOW);
    //buffering delay
    delay(200);
    digitalWrite(cokePin,HIGH);
    //Pours coke for 5 sec
    delay(5000);
    digitalWrite(cokePin, LOW);
    //Monitor Stop Timing
    delay(500);
    drinkFin=1;
    drinkReady=0;
  }
  
  
  
  //Take Arm Home
  
  if (drinkFin == 1) {
    drink1=0;
    drink2=0;
    drink3=0;
    drink4=0;
    motorMoveRev=1;

  }
  



}
  
  
  
  
