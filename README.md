# 🤖 AI-Powered Selenium Test Generator (Gemini 2.5 Flash Lite)

This project is an intelligent **AI-driven Selenium automation framework** that uses  
**Google Gemini 2.5 Flash Lite** to automatically generate Selenium steps and execute them in real browsers.

It dynamically converts AI-generated text into structured automation steps and performs actions such as:

- Opening URLs  
- Typing into fields  
- Clicking elements  
- Auto-correcting broken locators  
- Taking screenshots  
- Generating HTML reports  

This framework helps testers, QA engineers, and automation teams create **self-generating test cases** using AI.

---

## 🚀 Features

### 🔹 1. **AI-Generated Selenium Steps**
Uses Gemini 2.5 Flash Lite to convert natural language instructions into structured automation steps.

### 🔹 2. **Ultra Smart Locator Engine**
Automatically detects and corrects:
- id=
- name=
- class=
- xpath=
- css=
- Incorrect locators (auto-healing)

### 🔹 3. **Automatic 5-Second Wait**
Before every action:
```java
Thread.sleep(5000);
