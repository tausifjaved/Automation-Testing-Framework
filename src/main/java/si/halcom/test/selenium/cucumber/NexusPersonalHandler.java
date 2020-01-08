package si.halcom.test.selenium.cucumber;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;

import si.halcom.test.selenium.jna.Psapi;

public class NexusPersonalHandler {

    private static final int MAX_TITLE_LENGTH = 1024;

    public static boolean isPersonalPlugoutWinOpen() {

        if (Platform.isWindows()) {
            final int PROCESS_VM_READ = 0x0010;
            final int PROCESS_QUERY_INFORMATION = 0x0400;
            final User32 user32 = User32.INSTANCE;
            final Kernel32 kernel32 = Kernel32.INSTANCE;
            final Psapi psapi = Psapi.INSTANCE;
            WinDef.HWND windowHandle = user32.GetForegroundWindow();
            IntByReference pid = new IntByReference();
            user32.GetWindowThreadProcessId(windowHandle, pid);
            WinNT.HANDLE processHandle = kernel32.OpenProcess(PROCESS_VM_READ | PROCESS_QUERY_INFORMATION, true, pid.getValue());

            byte[] filename = new byte[512];
            Psapi.INSTANCE.GetModuleBaseNameW(processHandle.getPointer(), Pointer.NULL, filename, filename.length);
            String name = new String(filename);
            name = name.replaceAll("[\\s|\\u00A0]+", "");
            System.out.println("Active proces file name" + name);

            char[] buffer = new char[MAX_TITLE_LENGTH * 2];
            User32.INSTANCE.GetWindowText(windowHandle, buffer, MAX_TITLE_LENGTH);
            String title = Native.toString(buffer);
            System.out.println("Active window title: " + title);

            if ("Sign".equals(title)) {
                return true;

                

            } else {
                return false;
            }
        }

        if (Platform.isMac()) {
            final String script = "tell application \"System Events\"\n" + "\tname of application processes whose frontmost is tru\n" + "end";
            ScriptEngine appleScript = new ScriptEngineManager().getEngineByName("AppleScript");
            String result;
            try {
                result = (String) appleScript.eval(script);
                System.out.println(result);

            } catch (ScriptException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return false;
    }

}
