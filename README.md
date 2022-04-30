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
Start the activity responsible for camera live mode from your activity.
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
        hashMap?.forEach{key,value -> builder.append("\n$key : $value")}
        txtView.text = builder.toString()
    }
}
```
### 2. Digitally Created and Scanned PDFs
Add the following permission in the manifest file.
```
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
Load a PDF from your storage.
```
fun loadPdf(view: View){
    val browseStorage = Intent(Intent.ACTION_GET_CONTENT)
    browseStorage.type = "application/pdf"
    browseStorage.addCategory(Intent.CATEGORY_OPENABLE)
    startActivityForResult(Intent.createChooser(browseStorage, "Select PDF"), 100)
}
```
To receive the information extracted in the initial activity and present it in a TextView.
```
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null){
        val selectedPdf = data.data

        selectedPdf?.let {
            val builder = StringBuilder()
            textExtraction.processPdfBox(it) { hash ->
                var hashMap = hash
                hashMap?.forEach{key,value -> builder.append("\n$key : $value") }
                txtView.text = builder.toString()}
        }
    }
}
```
### 2. Digitally Created and Scanned PDFs
Add the following permission in the manifest file.
```
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
Resquest Permission.
```
override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    when(requestCode){
        PERMISSION_CODE -> {
            if( grantResults.size > 0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                pickImage()
            }else{
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
```
Load a Image from your storage.
```
fun loadImg(view: View){
    if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
            val permissions = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            requestPermissions(permissions, PERMISSION_CODE)
        }else{
            pickImage()
        }
    }else{
        pickImage()
    }
}

fun pickImage(){
    val intent = Intent(Intent.ACTION_PICK)
    intent.type = "image/*"
    startActivityForResult(intent, IMAGE_PICK_CODE)
}
```
To receive the information extracted in the initial activity and present it in a TextView.
```
override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
  if (resultCode == Activity.RESULT_OK && requestCode == IMAGE_PICK_CODE){
        var uri = data?.data
        val image = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        val builder = StringBuilder()
        textExtraction.realTimeProcess(image, 0) { hash ->
            var hashMap = hash
            if (hashMap.isEmpty()){
               txtView.setText("Nothing Detected")
            }else{
                hashMap?.forEach{key,value -> builder.append("\n$key : $value") }
                txtView.text = builder.toString()}
        }
    }
}
```
