package org.l2explorer.launcher.backend.instancemanager;

import org.l2explorer.launcher.backend.model.pipeline.enums.DownloadFailureType;
import org.l2explorer.launcher.backend.model.pipeline.enums.TaskStage;
import org.l2explorer.launcher.backend.model.pipeline.request.AbstractFileRequest;
import org.l2explorer.launcher.backend.model.pipeline.retry.NoRetryHandler;
import org.l2explorer.launcher.backend.util.concurrent.PipelineExecutors;

/**
 * EN: Store stage (last in the chain). Finalises the request to {@code DOWNLOAD_PATH/linkPath} via its
 *     {@code store()}, wrapped in {@code managedBlock}. On success the base success handler marks the task
 *     DONE and reports it to the sink. IO errors are terminal ({@link NoRetryHandler}) and are recovered by a
 *     later {@code -restore} run.<br>
 * RU: Стадия сохранения (последняя в цепочке). Финализирует запрос в {@code DOWNLOAD_PATH/linkPath} через его
 *     {@code store()}, обёрнутый в {@code managedBlock}. При успехе базовый обработчик помечает задачу DONE и
 *     сообщает приёмнику. Ошибки IO неустранимы ({@link NoRetryHandler}) и исправляются последующим запуском
 *     {@code -restore}.<br>
 **/
public class StoreStageManager extends AbstractStageManager
{
    private StoreStageManager()
    {
        super(NoRetryHandler.INSTANCE);
    }

    @Override
    protected TaskStage stage()
    {
        return TaskStage.STORE;
    }

    /**
     * EN: Finalises the request to disk via {@link org.l2explorer.launcher.backend.interfaces.IStoreRequest#store()},
     *     wrapped in a {@code ManagedBlocker} so the dedicated pool stays accounted during the blocking write.
     *     The target resolution, zip-slip guard and the actual write live in the request, so memory and temp
     *     storage differ only in how {@code store()} finalises. <br>
     * RU: Финализирует запрос на диск через
     *     {@link org.l2explorer.launcher.backend.interfaces.IStoreRequest#store()}, обёрнуто в {@code ManagedBlocker},
     *     чтобы выделенный пул корректно учитывался во время блокирующей записи. Разрешение цели, защита от
     *     zip-slip и сама запись находятся в запросе, поэтому память и временный файл различаются лишь тем, как
     *     финализирует {@code store()}. <br>
     * ==================================================================<br>
     * EN: @param task the request to store / RU: @param task запрос для сохранения <br>
     **/
    @Override
    protected void processTask(AbstractFileRequest task) throws Exception
    {
        PipelineExecutors.managedBlock(() ->
        {
            task.store();
            return Boolean.TRUE;
        });
    }

    @Override
    protected DownloadFailureType classify(Throwable throwable)
    {
        return DownloadFailureType.STORE_ERROR;
    }

    private static final StoreStageManager INSTANCE = new StoreStageManager();

    public static StoreStageManager getInstance()
    {
        return INSTANCE;
    }
}
