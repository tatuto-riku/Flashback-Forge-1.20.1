package com.moulberry.flashback.exporting.taskbar;

import com.moulberry.flashback.Flashback;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Guid;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.W32Errors;
import com.sun.jna.platform.win32.WTypes;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.ptr.PointerByReference;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFWNativeWin32;

import java.util.Locale;

final class TaskbarHost {

    private static final boolean WINDOWS = System.getProperty("os.name", "")
        .toLowerCase(Locale.ROOT).contains("win");
    private static final ThreadLocal<Boolean> COM_INITIALIZED = ThreadLocal.withInitial(() -> false);

    static ITaskbar createTaskbar() {
        if (!WINDOWS) {
            return new NoopTaskbar();
        }
        try {
            if (!COM_INITIALIZED.get()) {
                if (W32Errors.FAILED(Ole32.INSTANCE.CoInitializeEx(Pointer.NULL, Ole32.COINIT_MULTITHREADED))) {
                    throw new IllegalStateException("Failed to initialize COM for taskbar progress");
                }
                COM_INITIALIZED.set(true);
            }
            PointerByReference taskbarPointer = new PointerByReference();
            if (W32Errors.FAILED(Ole32.INSTANCE.CoCreateInstance(
                new Guid.GUID("56FDF344-FD6D-11d0-958A-006097C9A090"), null,
                WTypes.CLSCTX_SERVER,
                new Guid.GUID("EA1AFB91-9E28-4B86-90E9-9E9F8A5EEFAF"), taskbarPointer))) {
                throw new IllegalStateException("Failed to create ITaskbarList3");
            }

            long glfwWindow = Minecraft.getInstance().getWindow().getWindow();
            WinDef.HWND window = new WinDef.HWND(new Pointer(GLFWNativeWin32.glfwGetWin32Window(glfwWindow)));
            return new WindowsTaskbar(taskbarPointer.getValue(), window);
        } catch (Throwable throwable) {
            Flashback.LOGGER.warn("Unable to enable Windows taskbar export progress", throwable);
            return new NoopTaskbar();
        }
    }

    static void releaseThread() {
        if (COM_INITIALIZED.get()) {
            Ole32.INSTANCE.CoUninitialize();
            COM_INITIALIZED.remove();
        }
    }
}
