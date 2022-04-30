[![Maven Central](https://img.shields.io/maven-central/v/io.github.pedrolobo98/SmH-BloodAnalysis-Library.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.github.pedrolobo98%22%20AND%20a:%22SmH-BloodAnalysis-Library%22)

# SmH-BloodAnalysis-Library
SmH-BloodAnalysis-Library is an android library for extracting blood compounds and their concentrations from blood analysis documents. It was developed on top of the text recognition MLkit and PDFbox. This library offers the functionality to extract information from physical documents through camera live mode, digital created PDFs, scanned PDFs and images.

![Imagem1](https://user-images.githubusercontent.com/57667127/166104846-6777438c-ed9c-4735-b8ce-e48ed0294b92.png)

## Integration
Add de dependencie to the build.gradle file.

```
dependencies {
   implementation 'io.github.pedrolobo98:SmH-BloodAnalysis-Library:1.0.12'
}
```
## Usage
In this section it will be explained how to use the extraction of blood compounds and their concentrations from the different document formats. These are organized into 3 subtopics so that you can add to your projects only the format of documents you want to read.

### 1. Camera Live Mode
Add the following permission in the manifest file.
```
<uses-permission android:name="android.permission.CAMERA" />
```
Start the activity responsible for camera live mode from your activity
```
val intent = Intent(this, CameraActivity::class.java)
val activityName = this::class.java.name
intent.putExtra(Utils().homeActivityKey, activityName)
startActivity(intent)
```
To receive the information extracted in the initial activity and present it in a TextView.
```
override fun onResume() {
        super.onResume()

        if (intent.getSerializableExtra(Utils().savedUri) != null){
            val hashMap = intent.getSerializableExtra(Utils().savedUri) as HashMap<String, String>?

            val builder = StringBuilder()

            // Loop through the linked hash map
            hashMap?.forEach{key,value ->
                builder.append("\n$key : $value")
            }
            txtView.text = builder.toString()
        }
    }
```


