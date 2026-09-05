package com.aitoolbox.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val content: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("home") }

    if (currentScreen == "home") {
        HomeScreen(onNavigateToChat = { currentScreen = "chat" })
    } else if (currentScreen == "chat") {
        BackHandler { currentScreen = "home" }
        ChatScreen(onBack = { currentScreen = "home" })
    }
}

@Composable
fun HomeScreen(onNavigateToChat: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "AI Toolbox Pro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "网络已连通 · 云端引擎就绪",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("本地离线推理引擎", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("拍照文字识别 (OCR)", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onNavigateToChat,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("云端模型对话 💬", fontSize = 16.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(onBack: () -> Unit) {
    var apiKey by remember { mutableStateOf("") }
    var showKeyDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = { Text("设置 API Key") },
            text = {
                Column {
                    Text("输入你的 DeepSeek 或兼容 OpenAI 的 API Key：", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it.trim() },
                        placeholder = { Text("sk-...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("保存")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("云端 AI 对话", fontSize = 18.sp) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("返回")
                    }
                },
                actions = {
                    TextButton(onClick = { showKeyDialog = true }) {
                        Text(if (apiKey.isEmpty()) "填写 Key" else "已配置 Key")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "点击右上角配置 Key 后即可开始对话\n默认支持 DeepSeek 官方接口",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                items(messages) { msg ->
                    val isUser = msg.role == "user"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .background(
                                    color = if (isUser) MaterialTheme.colorScheme.primary else Color(0xFFF0F0F0),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.content,
                                color = if (isUser) Color.White else Color.Black,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
                if (isLoading) {
                    item {
                        Text("AI 正在思考中...", color = Color.Gray, fontSize = 13.sp)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("说点什么...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        val currentText = inputText.trim()
                        if (currentText.isNotEmpty() && !isLoading) {
                            if (apiKey.isEmpty()) {
                                showKeyDialog = true
                                return@Button
                            }
                            messages.add(ChatMessage("user", currentText))
                            inputText = ""
                            isLoading = true

                            scope.launch {
                                listState.animateScrollToItem(messages.size - 1)
                                try {
                                    val reply = withContext(Dispatchers.IO) {
                                        val jsonBody = JSONObject().apply {
                                            put("model", "deepseek-chat")
                                            val arr = JSONArray()
                                            messages.forEach { m ->
                                                arr.put(JSONObject().apply {
                                                    put("role", m.role)
                                                    put("content", m.content)
                                                })
                                            }
                                            put("messages", arr)
                                        }

                                        val request = Request.Builder()
                                            .url("https://api.deepseek.com/chat/completions")
                                            .addHeader("Authorization", "Bearer $apiKey")
                                            .addHeader("Content-Type", "application/json")
                                            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                                            .build()

                                        client.newCall(request).execute().use { response ->
                                            if (!response.isSuccessful) {
                                                "请求失败：HTTP ${response.code}"
                                            } else {
                                                val bodyStr = response.body?.string().orEmpty()
                                                val root = JSONObject(bodyStr)
                                                root.getJSONArray("choices")
                                                    .getJSONObject(0)
                                                    .getJSONObject("message")
                                                    .getString("content")
                                            }
                                        }
                                    }
                                    messages.add(ChatMessage("assistant", reply))
                                } catch (e: Exception) {
                                    messages.add(ChatMessage("assistant", "出错了：${e.message}"))
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("发送")
                }
            }
        }
    }
}
