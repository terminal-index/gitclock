package com.terminalindex.gitclock.server

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.terminalindex.gitclock.data.ComponentId
import com.terminalindex.gitclock.data.ComponentLayout
import com.terminalindex.gitclock.data.UserPreferences
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.response.respondText
import io.ktor.server.application.call
import io.ktor.http.ContentType
import io.ktor.server.request.receiveParameters
import io.ktor.server.request.receiveText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class GitClockServer(
    private val context: Context,
    private val userPrefs: UserPreferences,
    private val scope: CoroutineScope,
    private val screenSize: StateFlow<Pair<Int, Int>>
) {
    private var server: io.ktor.server.engine.ApplicationEngine? = null
    private val gson = Gson()

    fun start() {
        if (server != null) return
        
        scope.launch(Dispatchers.IO) {
            try {
                server = embeddedServer(Netty, port = 8080) {
                    routing {
                        get("/") {
                            call.respondText(getIndexHtml(), ContentType.Text.Html)
                        }
                        
                        get("/api/state") {
                            val size = screenSize.value
                            val layoutJson = userPrefs.layoutConfig.first() ?: "{}"
                            val layoutMap = try {
                                val type = object : TypeToken<Map<ComponentId, ComponentLayout>>() {}.type
                                gson.fromJson<Map<ComponentId, ComponentLayout>>(layoutJson, type) ?: emptyMap()
                            } catch(e: Exception) { emptyMap() }
                            
                            val response = mapOf(
                                "width" to size.first,
                                "height" to size.second,
                                "layout" to layoutMap
                            )
                            call.respondText(gson.toJson(response), ContentType.Application.Json)
                        }
                        
                        post("/api/layout") {
                            try {
                                val body = call.receiveText()
                                userPrefs.saveLayout(body)
                                call.respondText("OK")
                            } catch (e: Exception) {
                                call.respondText("Error: ${e.message}")
                            }
                        }

                        post("/api/reset_layout") {
                            userPrefs.saveLayout("{}")
                            call.respondText("OK")
                        }
                        
                        post("/settings") {
                            val params = call.receiveParameters()
                            val username = params["username"]
                            val token = params["token"]
                            val batteryStyle = params["batteryStyle"]?.toIntOrNull()
                            val oledMode = params["oledMode"] != null 
                            val keepScreenOn = params["keepScreenOn"] != null
                            
                            if (!username.isNullOrBlank()) {
                                userPrefs.saveCredentials(username, token ?: "")
                            }
                            if (batteryStyle != null) {
                                userPrefs.saveBatteryStyle(batteryStyle)
                            }
                            
                            userPrefs.saveOledMode(oledMode)
                            userPrefs.saveKeepScreenOn(keepScreenOn)
                            
                            call.respondText("Saved", ContentType.Text.Plain)
                        }
                    }
                }.start(wait = true)
            } catch (e: Exception) {
                Log.e("GitClockServer", "Failed to start server", e)
            }
        }
    }

    fun stop() {
        server?.stop(1000, 1000)
        server = null
    }

    private fun getIndexHtml(): String {
        return runBlocking {
            val currentSize = screenSize.value
            val oled = userPrefs.oledMode.first()
            val keepOn = userPrefs.keepScreenOn.first()
            val style = userPrefs.batteryStyle.first()
            
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val battLevel = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            
            val model = Build.MODEL
            val androidVer = Build.VERSION.RELEASE
            
            """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>GitClock</title>
                <link href="https://fonts.googleapis.com/css2?family=Google+Sans:wght@400;500;700&family=Roboto:wght@400;500&display=swap" rel="stylesheet">
                <style>
                    :root {
                        --bg-color: #32302f;
                        --surface-color: #3c3836;
                        --primary-color: #a89984; 
                        --accent-color: #b8bb26; 
                        --accent-secondary: #fabd2f; 
                        --text-primary: #ebdbb2;
                        --text-secondary: #a89984;
                        --border-color: #504945;
                    }

                    body {
                        font-family: 'Google Sans', 'Roboto', sans-serif;
                        background-color: var(--bg-color);
                        color: var(--text-primary);
                        margin: 0;
                        padding: 0;
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        min-height: 100vh;
                    }

                    header {
                        width: 100%;
                        padding: 20px;
                        background: rgba(60, 56, 54, 0.95);
                        backdrop-filter: blur(10px);
                        border-bottom: 1px solid var(--border-color);
                        text-align: center;
                        position: sticky;
                        top: 0;
                        z-index: 100;
                    }

                    h1 { margin: 0; font-weight: 500; letter-spacing: 0.5px; }
                    a.title-link { color: var(--accent-secondary); text-decoration: none; transition: color 0.2s; }
                    a.title-link:hover { color: var(--accent-color); }

                    .container {
                        display: flex;
                        flex-direction: column;
                        gap: 32px;
                        width: 100%;
                        max-width: 1200px;
                        padding: 32px;
                        box-sizing: border-box;
                        flex: 1;
                    }

                    .card {
                        background: var(--surface-color);
                        border-radius: 12px;
                        padding: 32px;
                        box-shadow: 0 4px 12px rgba(0,0,0,0.2);
                        border: 1px solid var(--border-color);
                    }

                    .form-grid {
                        display: grid;
                        grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
                        gap: 20px;
                        align-items: end;
                    }

                    .input-group { display: flex; flex-direction: column; gap: 8px; }
                    .checkbox-group { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
                    label { color: var(--text-secondary); font-size: 0.9em; font-weight: 500; }
                    
                    input[type=text], input[type=password], select {
                        background: #504945;
                        border: 1px solid #665c54;
                        color: var(--text-primary);
                        padding: 12px;
                        border-radius: 8px;
                        font-size: 1rem;
                        width: 100%;
                        box-sizing: border-box;
                    }
                    
                    input[type=checkbox] {
                        transform: scale(1.2);
                        accent-color: var(--accent-color);
                    }

                    button {
                        background: var(--accent-color);
                        color: #282828; 
                        border: none;
                        padding: 12px 32px;
                        border-radius: 8px;
                        font-weight: 700;
                        cursor: pointer;
                        transition: transform 0.2s;
                    }
                    button:hover { filter: brightness(1.1); transform: translateY(-1px); }
                    
                    .btn-danger {
                        background: #cc241d;
                        color: #ebdbb2;
                        font-size: 0.9rem;
                        padding: 10px 20px;
                        border-radius: 6px;
                        cursor: pointer;
                        border: none;
                        font-weight: 600;
                    }
                    .btn-danger:hover { background: #fb4934; }

                    .editor-section {
                        display: flex;
                        flex-direction: column;
                        align-items: center;
                        gap: 16px;
                    }

                    .device-frame {
                        background: #1d2021;
                        border: 8px solid #504945;
                        border-radius: 24px;
                        position: relative;
                        overflow: hidden; 
                        box-shadow: 0 20px 50px rgba(0,0,0,0.5);
                        transition: width 0.3s, height 0.3s;
                    }

                    .component {
                        position: absolute;
                        background: rgba(184, 187, 38, 0.2); 
                        border: 1px solid #b8bb26;
                        border-radius: 6px;
                        cursor: grab;
                        display: flex; 
                        align-items: center; 
                        justify-content: center;
                        color: #ebdbb2;
                        font-weight: 500;
                        font-size: 14px;
                        box-sizing: border-box;
                        z-index: 10;
                        transition: border-color 0.2s;
                    }
                    .component:hover { border-color: #fabd2f; }
                    .component.selected { 
                        border-color: #fe8019; 
                        background: rgba(254, 128, 25, 0.2); 
                        box-shadow: 0 0 15px rgba(254, 128, 25, 0.3);
                        z-index: 100 !important;
                    }

                    [data-id="STATS"] { z-index: 50; }
                    [data-id="COMMIT_BOARD"] { z-index: 5; }

                    .controls-overlay {
                        position: fixed;
                        bottom: 90px;
                        background: var(--surface-color);
                        padding: 16px 24px;
                        border-radius: 50px;
                        border: 1px solid var(--border-color);
                        display: flex;
                        gap: 20px;
                        align-items: center;
                        box-shadow: 0 10px 30px rgba(0,0,0,0.5);
                        transform: translateY(150%);
                        transition: transform 0.3s;
                        z-index: 200;
                    }
                    .controls-overlay.visible { transform: translateY(0); }
                    .scale-control { display: flex; align-items: center; gap: 12px; }
                    
                    footer {
                        width: 100%;
                        padding: 24px;
                        text-align: center;
                        color: #665c54;
                        font-size: 0.85em;
                        border-top: 1px solid var(--border-color);
                        margin-top: 40px;
                    }
                    
                </style>
            </head>
            <body>
                <header>
                    <h1><a href="https://github.com/terminal-index/gitclock" target="_blank" class="title-link">GitClock</a></h1>
                </header>

                <div class="container">
                    <div class="card">
                        <form action="/settings" method="post" id="settings-form">
                            <div class="form-grid">
                                <div class="input-group">
                                    <label>GitHub Username</label>
                                    <input type="text" name="username" placeholder="e.g. terminalindex" onchange="saveSettings()">
                                </div>
                                <div class="input-group">
                                    <label>GitHub OAuth Token</label>
                                    <input type="password" name="token" placeholder="ghp_..." onchange="saveSettings()">
                                </div>
                                
                                <div class="input-group">
                                    <label>Battery Style</label>
                                    <select name="batteryStyle" id="batt-select" onchange="saveSettings()">
                                        <option value="0">Default style</option>
                                        <option value="1">iOS style</option>
                                        <option value="2">Circle style</option>
                                    </select>
                                </div>
                                
                                <div class="input-group" style="justify-content:center;">
                                    <div class="checkbox-group">
                                        <input type="checkbox" name="oledMode" id="oled-check" onchange="saveSettings()">
                                        <label for="oled-check" style="margin:0; color:var(--text-primary);">OLED mode</label>
                                    </div>
                                    <div class="checkbox-group">
                                        <input type="checkbox" name="keepScreenOn" id="keep-check" onchange="saveSettings()">
                                        <label for="keep-check" style="margin:0; color:var(--text-primary);">Keep screen on</label>
                                    </div>
                                </div>
                            </div>
                        </form>
                        <div style="margin-top:24px; display:flex; justify-content:flex-end; border-top:1px solid var(--border-color); padding-top:16px;">
                            <button type="button" class="btn-danger" onclick="resetLayout()">Reset Layout</button>
                        </div>
                    </div>

                    <div class="editor-section">
                        <div style="display:flex; gap:16px; align-items:center;">
                            <h2 style="margin:0; font-size:1.4em; color:var(--text-primary);">Layout Editor</h2>
                            <span style="font-size:0.9em; color:var(--text-secondary);">(Live Preview)</span>
                        </div>
                        <div id="device-screen" class="device-frame"></div>
                    </div>
                </div>
                
                <div id="active-controls" class="controls-overlay">
                    <span id="selected-name" style="font-weight:bold; color:var(--accent-color);">Component</span>
                    <div class="scale-control">
                        <label>Size</label>
                        <input type="range" id="scale-slider" min="0.5" max="3.0" step="0.1" value="1.0" oninput="updateScale(this.value)">
                        <span id="scale-val" style="width:30px; text-align:right;">1.0</span>
                    </div>
                </div>
                
                <footer>
                    $model | Android $androidVer | Made with 🤎 by Terminal-Index
                </footer>

                <script>
                    const screen = document.getElementById('device-screen');
                    const controls = document.getElementById('active-controls');
                    const selectedName = document.getElementById('selected-name');
                    const scaleSlider = document.getElementById('scale-slider');
                    const scaleVal = document.getElementById('scale-val');

                    let layoutData = {};
                    let selectedId = null;
                    let screenW = ${currentSize.first};
                    let screenH = ${currentSize.second};
                    
                    document.getElementById('batt-select').value = "$style";
                    document.getElementById('oled-check').checked = $oled;
                    document.getElementById('keep-check').checked = $keepOn;
                    
                    const CONFIG = {
                        'BATTERY': { anchorX: 0, anchorY: 0, w: 200, h: 80 },
                        'CLOCK': { anchorX: 1, anchorY: 0, w: 300, h: 150 },
                        'STATS': { anchorX: 0.5, anchorY: 1, w: 350, h: 70 },
                        'COMMIT_BOARD': { anchorX: 0.5, anchorY: 1, w: 800, h: 250 } 
                    };
                    
                    const DEFAULTS = {
                        'BATTERY': { x: 0, y: 0 },
                        'CLOCK': { x: 0, y: 200 },
                        'STATS': { x: 0, y: -210 },
                        'COMMIT_BOARD': { x: 0, y: 0 }
                    };
                    
                    function saveSettings() {
                        const form = document.getElementById('settings-form');
                        const fd = new FormData(form);
                        fetch(form.action, { method: 'POST', body: fd });
                    }
                    
                    async function resetLayout() {
                        if(confirm('Reset all component positions?')) {
                            await fetch('/api/reset_layout', { method: 'POST' });
                            window.location.reload();
                        }
                    }

                    async function loadState() {
                        try {
                            const res = await fetch('/api/state');
                            const data = await res.json();
                            
                            if (Math.abs(data.width - screenW) > 10 || Math.abs(data.height - screenH) > 10) {
                                screenW = data.width || screenW;
                                screenH = data.height || screenH;
                                updateScreenDims();
                            }
                            
                            if (!window.isDragging) {
                                layoutData = data.layout || {};
                                render();
                            }
                        } catch(e) { console.error(e); }
                    }

                    function updateScreenDims() {
                        const maxW = window.innerWidth * 0.8;
                        const maxH = window.innerHeight * 0.6;
                        let scale = Math.min(maxW / screenW, maxH / screenH);
                        if (scale > 1) scale = 1;
                        screen.style.width = (screenW * scale) + 'px';
                        screen.style.height = (screenH * scale) + 'px';
                        window.deviceScale = scale;
                    }

                    function render() {
                        screen.innerHTML = '';
                        Object.keys(CONFIG).forEach(id => {
                            const saved = layoutData[id];
                            const def = DEFAULTS[id];
                            
                            const conf = saved ? saved : { id: id, x: def.x, y: def.y, scale: 1 };
                            const meta = CONFIG[id];
                            
                            const el = document.createElement('div');
                            el.className = 'component';
                            el.innerText = id.replace('_', ' ');
                            el.dataset.id = id;
                            if (id === selectedId) el.classList.add('selected');
                            
                            let elW = meta.w;
                            let elH = meta.h;

                            if (id === 'COMMIT_BOARD') {
                                const availW = screenW - 40; 
                                elW = availW;
                                const tileSize = availW / 100; 
                                elH = tileSize * 7.2; 
                            }

                            el.style.width = (elW * window.deviceScale) + 'px';
                            el.style.height = (elH * window.deviceScale) + 'px';
                            el.style.fontSize = (14 * window.deviceScale) + 'px';

                            let deviceLeft = 0; 
                            let deviceTop = 0;

                            if (meta.anchorX === 0) deviceLeft = conf.x;
                            else if (meta.anchorX === 1) deviceLeft = screenW - elW + conf.x;
                            else deviceLeft = (screenW - elW) / 2 + conf.x;

                            if (meta.anchorY === 0) deviceTop = conf.y;
                            else deviceTop = screenH - elH + conf.y;

                            el.style.left = (deviceLeft * window.deviceScale) + 'px';
                            el.style.top = (deviceTop * window.deviceScale) + 'px';
                            el.style.transform = `scale(${'$'}{conf.scale})`;

                            el.onmousedown = (e) => startDrag(e, id, el, meta, elW, elH);
                            el.ontouchstart = (e) => {
                                const t = e.touches[0];
                                startDrag({clientX: t.clientX, clientY: t.clientY, preventDefault:()=>e.preventDefault()}, id, el, meta, elW, elH);
                            };

                            screen.appendChild(el);
                        });
                        
                        const setEl = document.createElement('div');
                        setEl.className = 'component';
                        setEl.style.border = '1px dashed rgba(235, 219, 178, 0.5)';
                        setEl.style.background = 'rgba(0,0,0,0.3)';
                        setEl.style.color = 'rgba(235, 219, 178, 0.5)';
                        setEl.innerHTML = '⚙';
                        setEl.style.display = 'flex';
                        setEl.style.alignItems = 'center';
                        setEl.style.justifyContent = 'center';
                        setEl.style.fontSize = (24 * window.deviceScale) + 'px';

                        const setSize = 60;
                        setEl.style.width = (setSize * window.deviceScale) + 'px';
                        setEl.style.height = (setSize * window.deviceScale) + 'px';

                        const setLeft = screenW - setSize - 24; 
                        const setTop = 24;

                        setEl.style.left = (setLeft * window.deviceScale) + 'px';
                        setEl.style.top = (setTop * window.deviceScale) + 'px';
                        setEl.style.zIndex = 0; 
                        setEl.title = "Settings Button Area (Fixed)"; 
                        screen.appendChild(setEl);
                    }

                    function selectComponent(id) {
                        selectedId = id;
                        render();
                        controls.classList.add('visible');
                        selectedName.innerText = id.replace('_', ' ');
                        const saved = layoutData[id];
                        const def = DEFAULTS[id];
                        const conf = saved ? saved : { scale: 1 };
                        scaleSlider.value = conf.scale || 1.0;
                        scaleVal.innerText = (conf.scale || 1.0).toFixed(1);
                    }
                    
                    function updateScale(val) {
                        if (!selectedId) return;
                        scaleVal.innerText = val;
                        if (!layoutData[selectedId]) {
                             const def = DEFAULTS[selectedId];
                             layoutData[selectedId] = { id: selectedId, x: def.x, y: def.y, scale: 1 };
                        }
                        layoutData[selectedId].scale = parseFloat(val);
                        render();
                        saveLayout(); 
                    }

                    function startDrag(e, id, el, meta, elW, elH) {
                        e.preventDefault();
                        window.isDragging = true;
                        selectComponent(id);

                        const startMouseX = e.clientX;
                        const startMouseY = e.clientY;
                        
                        const saved = layoutData[id];
                        const def = DEFAULTS[id];
                        const conf = saved ? {...saved} : { id: id, x: def.x, y: def.y, scale: 1 };
                        const startOffsetX = conf.x || 0;
                        const startOffsetY = conf.y || 0;

                        const onMove = (clientX, clientY) => {
                            const deltaX = (clientX - startMouseX) / window.deviceScale;
                            const deltaY = (clientY - startMouseY) / window.deviceScale;
                            conf.x = startOffsetX + deltaX;
                            conf.y = startOffsetY + deltaY;
                            
                            let deviceLeft = 0; 
                            let deviceTop = 0;

                            if (meta.anchorX === 0) deviceLeft = conf.x;
                            else if (meta.anchorX === 1) deviceLeft = screenW - elW + conf.x;
                            else deviceLeft = (screenW - elW) / 2 + conf.x;

                            if (meta.anchorY === 0) deviceTop = conf.y;
                            else deviceTop = screenH - elH + conf.y;

                            el.style.left = (deviceLeft * window.deviceScale) + 'px';
                            el.style.top = (deviceTop * window.deviceScale) + 'px';
                        };

                        const onEnd = () => {
                            window.isDragging = false;
                            document.removeEventListener('mousemove', docMove);
                            document.removeEventListener('mouseup', docUp);
                            document.removeEventListener('touchmove', docTouchMove);
                            document.removeEventListener('touchend', docTouchEnd);
                            layoutData[id] = conf;
                            saveLayout();
                        };

                        const docMove = (e) => onMove(e.clientX, e.clientY);
                        const docUp = () => onEnd();
                        document.addEventListener('mousemove', docMove);
                        document.addEventListener('mouseup', docUp);
                        const docTouchMove = (e) => { const t=e.touches[0]; onMove(t.clientX, t.clientY); };
                        const docTouchEnd = () => onEnd();
                        document.addEventListener('touchmove', docTouchMove, {passive:false});
                        document.addEventListener('touchend', docTouchEnd);
                    }

                    async function saveLayout() {
                        await fetch('/api/layout', { method: 'POST', body: JSON.stringify(layoutData) });
                    }

                    updateScreenDims();
                    render();
                    setInterval(loadState, 2000);
                </script>
            </body>
            </html>
            """
        }
    }
}
