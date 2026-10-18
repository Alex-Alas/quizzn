using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Text;
using System.IO;
using System.Runtime.InteropServices;
using System.Threading.Tasks;
using System.Windows.Forms;
using Microsoft.Web.WebView2.Core;
using Microsoft.Web.WebView2.WinForms;

// Quizz'n para Windows: pone la web de Quizz'n encima del juego cada N minutos.
// La web hace todo (biblioteca, preguntas, ajustes). Esta app solo pone la ventana encima, le manda Esc
// al juego y conversa con la web: app → web llama overlayBreak()/home(); web → app manda "cfg:min:esc", "now", "done".
static class Program
{
    [DllImport("user32.dll")] static extern bool SetProcessDPIAware();

    [STAThread]
    static void Main(string[] args)
    {
        using var once = new System.Threading.Mutex(true, "Quizzn.single", out var first);
        if (!first) { MessageBox.Show("Quizz'n ya está abierto: búscalo en la bandeja, junto al reloj.", "Quizz'n"); return; }
        SetProcessDPIAware();
        Application.EnableVisualStyles();
        // argumento opcional: otra URL (p. ej. http://localhost:8765/ para desarrollo)
        Application.Run(new Host(args.Length > 0 ? args[0] : "https://alex-alas.github.io/quizzn/"));
    }
}

class Host : Form
{
    [DllImport("user32.dll")] static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] static extern uint GetWindowThreadProcessId(IntPtr h, IntPtr pid);
    [DllImport("user32.dll")] static extern bool AttachThreadInput(uint a, uint b, bool attach);
    [DllImport("kernel32.dll")] static extern uint GetCurrentThreadId();
    [DllImport("user32.dll")] static extern void keybd_event(byte vk, byte scan, uint flags, UIntPtr extra);
    [DllImport("user32.dll")] static extern bool RegisterHotKey(IntPtr h, int id, uint mods, uint vk);

    readonly WebView2 web = new WebView2 { Dock = DockStyle.Fill };
    readonly NotifyIcon tray = new NotifyIcon { Text = "Quizz'n", Visible = true };
    readonly Timer timer = new Timer { Interval = 20 * 60000 };
    readonly string url;
    IntPtr game;
    bool esc = true, inBreak, hinted;

    public Host(string url)
    {
        this.url = url;
        Text = "Quizz'n";
        Icon = tray.Icon = MakeIcon();
        Controls.Add(web);
        NormalMode();

        var menu = new ContextMenuStrip();
        menu.Items.Add("Abrir Quizz'n", null, (s, e) => Open());
        menu.Items.Add("Pausa de estudio ahora (Ctrl+Alt+Q)", null, (s, e) => Break());
        menu.Items.Add("Salir", null, (s, e) => { tray.Visible = false; Application.Exit(); }); // salida de emergencia
        tray.ContextMenuStrip = menu;
        tray.DoubleClick += (s, e) => Open();

        timer.Tick += (s, e) => Break();
        timer.Start();
        Init();
    }

    async void Init()
    {
        try
        {
            var data = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Quizzn");
            await web.EnsureCoreWebView2Async(await CoreWebView2Environment.CreateAsync(null, data));
        }
        catch (WebView2RuntimeNotFoundException)
        {
            MessageBox.Show("Falta Microsoft Edge WebView2. Instálalo desde https://go.microsoft.com/fwlink/p/?LinkId=2124703 y vuelve a abrir Quizz'n.", "Quizz'n");
            tray.Visible = false;
            Application.Exit();
            return;
        }
        web.CoreWebView2.WebMessageReceived += (s, e) => OnMessage(e.TryGetWebMessageAsString());
        web.CoreWebView2.Navigate(url);
    }

    void OnMessage(string m)
    {
        var p = m.Split(':');
        if (p[0] == "cfg" && p.Length == 3 && int.TryParse(p[1], out var min))
        {
            timer.Interval = Math.Max(1, min) * 60000;
            esc = p[2] == "1";
        }
        else if (p[0] == "now") Break();
        else if (p[0] == "done") EndBreak();
    }

    async void Break()
    {
        if (inBreak || web.CoreWebView2 == null) return;
        inBreak = true;
        game = GetForegroundWindow();
        // si la web no cargó (sin internet), no tapamos el juego con una ventana que no se puede cerrar
        if (await web.ExecuteScriptAsync("typeof overlayBreak=='function'&&(overlayBreak(),1)") != "1") { inBreak = false; return; }
        timer.Stop();
        if (esc && game != Handle)
        {
            // Esc abre el menú de pausa en la mayoría de juegos; lo manda mientras el juego aún tiene el foco
            keybd_event(0x1B, 0x01, 0, UIntPtr.Zero);
            await Task.Delay(60);
            keybd_event(0x1B, 0x01, 2, UIntPtr.Zero);
            await Task.Delay(250);
        }
        WindowState = FormWindowState.Normal;
        FormBorderStyle = FormBorderStyle.None;
        Bounds = Screen.FromHandle(game).Bounds;
        Show();
        // Windows no deja robar el foco a una app de fondo; unirse al hilo de la ventana activa lo permite
        uint fg = GetWindowThreadProcessId(GetForegroundWindow(), IntPtr.Zero), me = GetCurrentThreadId();
        AttachThreadInput(fg, me, true);
        SetForegroundWindow(Handle);
        AttachThreadInput(fg, me, false);
        Activate();
        TopMost = true; // después de tomar el foco: antes, Windows ignora el "siempre encima" en silencio
        web.Focus();
    }

    void EndBreak()
    {
        if (!inBreak) return;
        inBreak = false;
        TopMost = false;
        timer.Start();
        if (game == Handle) { NormalMode(); return; } // fue "Probar ahora" desde la ventana normal
        Hide();
        SetForegroundWindow(game);
    }

    void Open()
    {
        if (inBreak) return;
        if (!Visible)
        {
            web.CoreWebView2?.ExecuteScriptAsync("home()");
            NormalMode();
        }
        Show();
        Activate();
    }

    void NormalMode()
    {
        StartPosition = FormStartPosition.Manual;
        FormBorderStyle = FormBorderStyle.Sizable;
        WindowState = FormWindowState.Normal;
        var area = Screen.PrimaryScreen.WorkingArea;
        Size = new Size(Math.Min(LogicalToDeviceUnits(480), area.Width), Math.Min(LogicalToDeviceUnits(800), area.Height));
        Location = new Point(area.Left + (area.Width - Width) / 2, area.Top + (area.Height - Height) / 2);
    }

    protected override void OnHandleCreated(EventArgs e)
    {
        base.OnHandleCreated(e);
        RegisterHotKey(Handle, 1, 0x1 | 0x2 | 0x4000, (uint)Keys.Q); // Ctrl+Alt+Q, sin repetir
    }

    protected override void WndProc(ref Message m)
    {
        if (m.Msg == 0x0312) Break(); // WM_HOTKEY
        base.WndProc(ref m);
    }

    protected override void OnFormClosing(FormClosingEventArgs e)
    {
        if (e.CloseReason == CloseReason.UserClosing)
        {
            e.Cancel = true; // cerrar = esconder en la bandeja; en plena pausa no se cierra (Alt+F4 tampoco)
            if (!inBreak)
            {
                Hide();
                if (!hinted)
                {
                    hinted = true;
                    tray.ShowBalloonTip(4000, "Quizz'n sigue aquí", "Las pausas de estudio salen solas. Ctrl+Alt+Q para una ahora; clic derecho en este ícono para salir.", ToolTipIcon.Info);
                }
            }
        }
        base.OnFormClosing(e);
    }

    static Icon MakeIcon()
    {
        var b = new Bitmap(64, 64);
        using (var g = Graphics.FromImage(b))
        using (var f = new Font("Segoe UI", 42, FontStyle.Bold, GraphicsUnit.Pixel))
        using (var fill = new SolidBrush(Color.FromArgb(255, 183, 3)))
        using (var ink = new SolidBrush(Color.FromArgb(27, 21, 0)))
        {
            g.SmoothingMode = SmoothingMode.AntiAlias;
            g.TextRenderingHint = TextRenderingHint.AntiAliasGridFit;
            g.FillEllipse(fill, 0, 0, 63, 63);
            g.DrawString("Q", f, ink, new RectangleF(0, 2, 64, 64), new StringFormat { Alignment = StringAlignment.Center, LineAlignment = StringAlignment.Center });
        }
        return Icon.FromHandle(b.GetHicon());
    }
}
