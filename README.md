
# react-native-acr35

## Getting started

`$ npm install react-native-acr35 --save`

### Mostly automatic installation

`$ react-native link react-native-acr35`

### Manual installation


#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-acr35` and add `RNAcr35.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNAcr35.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.coachhire.acr35.RNAcr35Package;` to the imports at the top of the file
  - Add `new RNAcr35Package()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-acr35'
  	project(':react-native-acr35').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-acr35/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-acr35')
  	```


## Usage
```javascript
import RNAcr35 from 'react-native-acr35';

// TODO: What to do with the module?
RNAcr35;
```
  