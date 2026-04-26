package com.example.mybasic.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.google.android.material.button.MaterialButton
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import java.io.File

class EditorActivity : AppCompatActivity() {
    
    private lateinit var editor: CodeEditor
    private var currentFile: File? = null
    private var fileContent: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        editor = findViewById(R.id.code_editor)
        
        // إعدادات المحرر
        setupEditor()
        
        // تحميل الملف إذا ورد
        val filePath = intent.getStringExtra("file_path") ?: "/sdcard/note.txt"
        currentFile = File(filePath)
        loadFile()
        
        // أزرار التحكم
        findViewById<MaterialButton>(R.id.btn_save).setOnClickListener { saveFile() }
        findViewById<MaterialButton>(R.id.btn_save_as).setOnClickListener { saveFileAs() }
        findViewById<MaterialButton>(R.id.btn_undo).setOnClickListener { editor.undo() }
        findViewById<MaterialButton>(R.id.btn_redo).setOnClickListener { editor.redo() }
        findViewById<MaterialButton>(R.id.btn_search).setOnClickListener { editor.searchOptions.show() }
    }
    
    private fun setupEditor() {
        // مظهر داكن
        val colorScheme = TextMateColorScheme.create()
        colorScheme.setColor(io.github.rosemoe.sora.widget.schemes.EditorColorScheme.WHOLE_BACKGROUND, 
                             android.graphics.Color.parseColor("#1E1E1E"))
        editor.colorScheme = colorScheme
        
        editor.setTextSize(14f)
        editor.isLineNumberEnabled = true
        editor.isWordwrap = false
        
        // تمييز للغات متعددة
        val language = detectLanguage(currentFile?.name)
        editor.setEditorLanguage(TextMateLanguage.create(language))
    }
    
    private fun detectLanguage(filename: String?): String {
        return when (filename?.substringAfterLast('.')) {
            "rs" -> "source.rust"
            "py" -> "source.python"
            "kt", "java" -> "source.kotlin"
            "sh" -> "source.shell"
            "js" -> "source.javascript"
            "html" -> "text.html.basic"
            else -> "text.plain"
        }
    }
    
    private fun loadFile() {
        currentFile?.let { file ->
            if (file.exists()) {
                fileContent = file.readText()
                editor.setText(fileContent)
                supportActionBar?.title = file.name
            } else {
                editor.setText("")
                supportActionBar?.title = "محرر جديد"
            }
        }
    }
    
    private fun saveFile(): Boolean {
        return currentFile?.let { file ->
            try {
                file.writeText(editor.text.toString())
                Toast.makeText(this, "تم الحفظ: ${file.name}", Toast.LENGTH_SHORT).show()
                true
            } catch (e: Exception) {
                Toast.makeText(this, "خطأ: ${e.message}", Toast.LENGTH_SHORT).show()
                false
            }
        } ?: saveFileAs()
    }
    
    private fun saveFileAs(): Boolean {
        // فتح نافذة لحفظ الملف
        val fileName = "document_${System.currentTimeMillis()}.txt"
        val newFile = File("/sdcard/$fileName")
        currentFile = newFile
        return saveFile()
    }
    
    override fun onSupportNavigateUp(): Boolean {
        if (editor.text.toString() != fileContent) {
            // ملف غير محفوظ - طلب تأكيد
            android.app.AlertDialog.Builder(this)
                .setTitle("حفظ التغييرات؟")
                .setMessage("هل تريد حفظ التغييرات قبل الخروج؟")
                .setPositiveButton("حفظ") { _, _ -> 
                    saveFile()
                    finish()
                }
                .setNegativeButton("تجاهل") { _, _ -> finish() }
                .setNeutralButton("إلغاء", null)
                .show()
        } else {
            finish()
        }
        return true
    }
        }
