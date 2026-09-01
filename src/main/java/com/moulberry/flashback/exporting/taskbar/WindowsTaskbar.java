package com.moulberry.flashback.exporting.taskbar;

import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.COM.COMInvoker;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;

final class WindowsTaskbar extends COMInvoker implements ITaskbar {

    private final WinDef.HWND window;

    WindowsTaskbar(Pointer pointer, WinDef.HWND window) {
        this.setPointer(pointer);
        this.window = window;
        this.invokeTaskbar(3); // ITaskbarList::HrInit
    }

    private void invokeTaskbar(int vtableEntry, Object... values) {
        Object[] arguments = new Object[values.length + 1];
        arguments[0] = this.getPointer();
        System.arraycopy(values, 0, arguments, 1, values.length);
        WinNT.HRESULT result = (WinNT.HRESULT) this._invokeNativeObject(vtableEntry, arguments,
            WinNT.HRESULT.class);
        if (W32Errors.FAILED(result)) {
            throw new IllegalStateException("Windows taskbar call failed: " + vtableEntry);
        }
    }

    @Override
    public void close() {
        this.reset();
        this.invokeTaskbar(2); // IUnknown::Release
    }

    @Override
    public void reset() {
        this.invokeTaskbar(10, this.window, 0); // TBPF_NOPROGRESS
    }

    @Override
    public void setProgress(long count, long outOf) {
        this.invokeTaskbar(9, this.window, count, outOf);
    }

    @Override
    public void setPaused() {
        this.invokeTaskbar(10, this.window, 8); // TBPF_PAUSED
    }

    @Override
    public void setNormal() {
        this.invokeTaskbar(10, this.window, 2); // TBPF_NORMAL
    }
}
