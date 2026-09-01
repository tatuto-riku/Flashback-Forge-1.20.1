package com.moulberry.flashback.exporting.taskbar;

import com.moulberry.flashback.Flashback;
import com.moulberry.flashback.editor.ui.windows.ExportDoneWindow;
import com.moulberry.flashback.exporting.ExportJob;

public final class TaskbarManager {

    private static ITaskbar taskbar;
    private static int lastCount = -1;
    private static int lastTotal = -1;
    private static long lastUpdate = -1;
    private static boolean lastPaused;
    private static volatile boolean started;

    private TaskbarManager() {}

    public static synchronized void launch() {
        if (started) {
            return;
        }
        started = true;
        Thread updater = new Thread(TaskbarManager::run, "Flashback-Taskbar-Updater");
        updater.setDaemon(true);
        updater.start();
    }

    private static void run() {
        try {
            while (Flashback.isInReplay()) {
                Thread.sleep(100L);
                tick();
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        } finally {
            if (taskbar != null) {
                try {
                    taskbar.close();
                } catch (Throwable ignored) {}
                taskbar = null;
            }
            TaskbarHost.releaseThread();
            started = false;
            lastCount = -1;
            lastTotal = -1;
        }
    }

    private static void tick() {
        ExportJob exportJob = Flashback.EXPORT_JOB;
        if (exportJob != null) {
            long now = System.currentTimeMillis();
            boolean unchanged = exportJob.progressCount == lastCount && exportJob.progressOutOf == lastTotal;
            if (!unchanged || lastUpdate < 0 || lastUpdate > now) {
                lastUpdate = now;
            }
            setProgress(exportJob.progressCount, exportJob.progressOutOf,
                unchanged && now - lastUpdate > 5000L);
        } else if (ExportDoneWindow.isDone()) {
            lastUpdate = -1;
            setProgress(1, 1, false);
        } else {
            lastUpdate = -1;
            setProgress(0, 0, false);
        }
    }

    private static void setProgress(int count, int total, boolean paused) {
        if (count == lastCount && total == lastTotal && paused == lastPaused) {
            return;
        }
        boolean wasInactive = lastCount <= 0 || lastTotal <= 0;
        if (taskbar == null) {
            taskbar = TaskbarHost.createTaskbar();
        }
        if (count <= 0 || total <= 0) {
            taskbar.reset();
            lastPaused = false;
        } else {
            if (wasInactive || paused != lastPaused) {
                if (paused) {
                    taskbar.setPaused();
                } else {
                    taskbar.setNormal();
                }
                lastPaused = paused;
            }
            taskbar.setProgress(count, total);
        }
        lastCount = count;
        lastTotal = total;
    }
}
