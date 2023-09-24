<p align="center">
  <img src="https://github.com/Nain57/SmartAutoClicker/blob/master/smartautoclicker/src/main/ic_smart_auto_clicker-playstore.png?raw=true" height="64">
  <h3 align="center">Smart AutoClicker</h3>
  <p align="center">An open-source auto clicker on images for Android<p>
</p>

<br>
 
## Introduction

Smart AutoClicker is an Android application allowing to automate repetitive task by clicking automatically for you on the screen. Unlike the regular auto clicker application, the clicks aren't based on timers to execute the clicks. Instead, it allows you to capture an image from a part of your screen and execute the click once this image is detected again.

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
     alt="Get it on F-Droid"
     height="80">](https://f-droid.org/packages/com.buzbuz.smartautoclicker/)

<br>

<p align="center">
  <img src="https://i.postimg.cc/65JBX8D9/Phone-Screenshot-1.png" width="200">
  <img src="https://i.postimg.cc/6Q3X0nGh/Phone-Screenshot-2.png" width="200">
  <img src="https://i.postimg.cc/1zjZYXG1/Phone-Screenshot-3.png" width="200">
  <img src="https://i.postimg.cc/qvp0N9JS/Phone-Screenshot-4.png" width="200">
</p>
<br>
<p align="center">
  <img src="https://i.postimg.cc/8zwGMts5/Phone-Screenshot-5.png" width="200">
  <img src="https://i.postimg.cc/ZnrtRL1J/Phone-Screenshot-6.png" width="200">
  <img src="https://i.postimg.cc/1Xx1sd7W/Phone-Screenshot-7.png" width="200">
  <img src="https://i.postimg.cc/nz9t8x2j/Phone-Screenshot-8.png" width="200">
</p>
<br>


## Features

- Organize clicks by scenario
- Execute clicks or swipes
- Add an image condition from the screen
- Modify the tolerance for the condition detection
- Combine multiple conditions
- Configure the delay before the next click
- Modify the prority order of a click

## Iteration planning
- 可以调整识别条件的顺序（高）✅
- 识别条件支持指定识别区域，目前是只允许选择识别条件坐标（还不能修改）和整个屏幕（高）✅
- 复用识别条件、操作时按照所属脚本分类展示（高）
- 识别条件和操作支持修改坐标（中）
- 支持调节每个识别条件的识别速度-精准度比值（中）
- 增加全局动作（Action）（中）
- 增加开始前置步骤（低）
- 识别条件和操作允许以坐标偏移比例的方式定义（低）
- 增加识别条件：时间、电量（低）
