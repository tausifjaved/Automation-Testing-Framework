package si.halcom.test.selenium.jna;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.win32.StdCallLibrary;

public interface Psapi extends StdCallLibrary {
    Psapi INSTANCE = (Psapi) Native.loadLibrary("Psapi", Psapi.class);

    WinDef.DWORD GetModuleBaseNameW(Pointer hProcess, Pointer hModule, byte[] lpBaseName, int nSize);
}
