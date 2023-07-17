package org.telegram.messenger;

import android.text.TextUtils;
import android.util.LongSparseArray;
import android.util.SparseArray;

import org.telegram.messenger.utils.AsyncTaskRunner;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Storage.CacheModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.Callable;

public class AccountClean extends AsyncTaskRunner<Void> {

    protected int currentAccount = UserConfig.selectedAccount;

    private final int TYPE_PHOTOS = 0;
    private final int TYPE_VIDEOS = 1;
    private final int TYPE_DOCUMENTS = 2;
    private final int TYPE_VOICE = 4;
    private final int TYPE_OTHER = 6;

    HashSet<Long> selectedDialogs = new HashSet<>();

    public static final long UNKNOWN_CHATS_DIALOG_ID = Long.MAX_VALUE;

    private boolean loadingDialogs;


    public AccountInstance getAccountInstance() {
        return AccountInstance.getInstance(currentAccount);
    }

    public MessagesStorage getMessagesStorage() {
        return getAccountInstance().getMessagesStorage();
    }

    public MediaDataController getMediaDataController() {
        return getAccountInstance().getMediaDataController();
    }

    public MessagesController getMessagesController() {
        return getAccountInstance().getMessagesController();
    }

    public FileLoader getFileLoader() {
        return getAccountInstance().getFileLoader();
    }

    private void clearDatabase() {
        getMessagesStorage().clearLocalDatabase();
    }

    private void cleanupFoldersInternal() {

        FileLoader.getInstance(currentAccount).clearFilePaths();
        FileLoader.getInstance(currentAccount).checkCurrentDownloadsFiles();

        AndroidUtilities.runOnUIThread(() -> {
            ImageLoader.getInstance().clearMemory();
            MediaController.getInstance().cleanup();
            getMediaDataController().ringtoneDataStore.checkRingtoneSoundsLoaded();
            MediaDataController.getInstance(currentAccount).chekAllMedia(true);
            MediaDataController.getInstance(currentAccount).cleanup();
            loadDialogEntities();
            FileLog.e("Load Dialog Entities Call");
        });
    }

    private void cleanupFolders() {
        getFileLoader().cancelLoadAllFiles();
        getFileLoader().getFileLoaderQueue().postRunnable(() -> Utilities.globalQueue.postRunnable(() -> {
            cleanupFoldersInternal();
            FileLog.e("Cleanup Folders Internal Call");
        }));
        dialogsFilesEntities = null;
    }


    ArrayList<DialogFileEntities> dialogsFilesEntities = null;

    public void fillDialogsEntitiesRecursive(final File fromFolder, int type, LongSparseArray<DialogFileEntities> dilogsFilesEntities, CacheModel cacheModel) {
        if (fromFolder == null) {
            return;
        }
        File[] files = fromFolder.listFiles();
        if (files == null) {
            return;
        }
        for (final File fileEntry : files) {
            boolean canceled = false;
            if (fileEntry.isDirectory()) {
                fillDialogsEntitiesRecursive(fileEntry, type, dilogsFilesEntities, cacheModel);
            } else {
                if (fileEntry.getName().equals(".nomedia")) {
                    continue;
                }
                FilePathDatabase.FileMeta fileMetadata = getFileLoader().getFileDatabase().getFileDialogId(fileEntry, null);
                int addToType = type;
                String fileName = fileEntry.getName().toLowerCase();
                if (fileName.endsWith(".mp3") || fileName.endsWith(".m4a")) {
                    addToType = 3;
                }
                CacheModel.FileInfo fileInfo = new CacheModel.FileInfo(fileEntry);
                fileInfo.type = addToType;
                if (fileMetadata != null) {
                    fileInfo.dialogId = fileMetadata.dialogId;
                    fileInfo.messageId = fileMetadata.messageId;
                    fileInfo.messageType = fileMetadata.messageType;
                }
                fileInfo.size = fileEntry.length();
                if (fileInfo.dialogId != 0) {
                    DialogFileEntities dilogEntites = dilogsFilesEntities.get(fileInfo.dialogId, null);
                    if (dilogEntites == null) {
                        dilogEntites = new DialogFileEntities(fileInfo.dialogId);
                        dilogsFilesEntities.put(fileInfo.dialogId, dilogEntites);
                    }
                    dilogEntites.addFile(fileInfo, addToType);
                    cleanupDialogFiles(dilogEntites);
                }
                if (cacheModel != null) {
                    cacheModel.add(addToType, fileInfo);
                }


                //TODO measure for other accounts
//                for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
//                    if (i != currentAccount && UserConfig.getInstance(currentAccount).isClientActivated()) {
//                        FileLoader.getInstance(currentAccount).getFileDatabase().getFileDialogId(fileEntry);
//                    }
//                }
            }
        }
    }

    private void sort(ArrayList<DialogFileEntities> entities) {
        Collections.sort(entities, (o1, o2) -> {
            if (o2.totalSize > o1.totalSize) {
                return 1;
            } else if (o2.totalSize < o1.totalSize) {
                return -1;
            }
            return 0;
        });
    }

    private void loadDialogEntities() {
        getFileLoader().getFileDatabase().getQueue().postRunnable(() -> {
            CacheModel cacheModel = new CacheModel(false);
            LongSparseArray<DialogFileEntities> dilogsFilesEntities = new LongSparseArray<>();

            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_CACHE), TYPE_OTHER, dilogsFilesEntities, null);

            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_IMAGE), TYPE_PHOTOS, dilogsFilesEntities, cacheModel);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_IMAGE_PUBLIC), TYPE_PHOTOS, dilogsFilesEntities, cacheModel);

            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_VIDEO), TYPE_VIDEOS, dilogsFilesEntities, cacheModel);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_VIDEO_PUBLIC), TYPE_VIDEOS, dilogsFilesEntities, cacheModel);

            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_AUDIO), TYPE_VOICE, dilogsFilesEntities, cacheModel);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_DOCUMENT), TYPE_DOCUMENTS, dilogsFilesEntities, cacheModel);
            fillDialogsEntitiesRecursive(FileLoader.checkDirectory(FileLoader.MEDIA_DIR_FILES), TYPE_DOCUMENTS, dilogsFilesEntities, cacheModel);

            ArrayList<DialogFileEntities> entities = new ArrayList<>();
            ArrayList<Long> unknownUsers = new ArrayList<>();
            ArrayList<Long> unknownChats = new ArrayList<>();
            for (int i = 0; i < dilogsFilesEntities.size(); i++) {
                DialogFileEntities dialogEntities = dilogsFilesEntities.valueAt(i);
                entities.add(dialogEntities);
                if (getMessagesController().getUserOrChat(entities.get(i).dialogId) == null) {
                    if (dialogEntities.dialogId > 0) {
                        unknownUsers.add(dialogEntities.dialogId);
                    } else {
                        unknownChats.add(dialogEntities.dialogId);
                    }
                }
            }
            cacheModel.sortBySize();
            getMessagesStorage().getStorageQueue().postRunnable(() -> {
                ArrayList<TLRPC.User> users = new ArrayList<>();
                ArrayList<TLRPC.Chat> chats = new ArrayList<>();
                if (!unknownUsers.isEmpty()) {
                    try {
                        getMessagesStorage().getUsersInternal(TextUtils.join(",", unknownUsers), users);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                if (!unknownChats.isEmpty()) {
                    try {
                        getMessagesStorage().getChatsInternal(TextUtils.join(",", unknownChats), chats);
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
                for (int i = 0; i < entities.size(); i++) {
                    if (entities.get(i).totalSize <= 0) {
                        entities.remove(i);
                        i--;
                    }
                }
                sort(entities);
                AndroidUtilities.runOnUIThread(() -> {
                    loadingDialogs = false;
                    getMessagesController().putUsers(users, true);
                    getMessagesController().putChats(chats, true);
                    DialogFileEntities unknownChatsEntity = null;
                    for (int i = 0; i < entities.size(); i++) {
                        DialogFileEntities dialogEntities = entities.get(i);
                        boolean changed = false;
                        if (getMessagesController().getUserOrChat(dialogEntities.dialogId) == null) {
                            dialogEntities.dialogId = UNKNOWN_CHATS_DIALOG_ID;
                            if (unknownChatsEntity != null) {
                                changed = true;
                                unknownChatsEntity.merge(dialogEntities);
                                entities.remove(i);
                                i--;
                            } else {
                                unknownChatsEntity = dialogEntities;
                            }
                            if (changed) {
                                sort(entities);
                            }
                        }
                    }
                });
            });
        });
    }

    private void cleanupDialogFiles(DialogFileEntities dialogEntities) {

        List<CacheModel.FileInfo> filesToRemove = new ArrayList<>();

        for (int a = 0; a < 7; a++) {

            FileEntities entitiesToDelete = dialogEntities.entitiesByType.get(a);

            FileLog.e("Entities to Delete: " + dialogEntities.entitiesByType.get(a).totalSize);

            filesToRemove.addAll(entitiesToDelete.files);
            dialogEntities.entitiesByType.delete(a);

        }
        if (dialogEntities.entitiesByType.size() == 0) {
            dialogsFilesEntities.remove(dialogEntities);
        }

        FileLog.e("Remove files count: " + filesToRemove.size());

        getFileLoader().getFileDatabase().removeFiles(filesToRemove);
        getFileLoader().cancelLoadAllFiles();
        getFileLoader().getFileLoaderQueue().postRunnable(() -> {
            for (int i = 0; i < filesToRemove.size(); i++) {
                filesToRemove.get(i).file.delete();
            }

            AndroidUtilities.runOnUIThread(() -> {
                FileLoader.getInstance(currentAccount).checkCurrentDownloadsFiles();
            });
        });
    }

    private static class DialogFileEntities {

        long dialogId;
        long totalSize;
        SparseArray<FileEntities> entitiesByType = new SparseArray<>();

        public DialogFileEntities(long dialogId) {
            this.dialogId = dialogId;
        }

        public void addFile(CacheModel.FileInfo file, int type) {
            FileEntities entities = entitiesByType.get(type, null);
            if (entities == null) {
                entities = new FileEntities();
                entitiesByType.put(type, entities);
            }
            entities.count++;
            long fileSize = file.size;
            entities.totalSize += fileSize;
            totalSize += fileSize;
            entities.files.add(file);
        }

        public void merge(DialogFileEntities dialogEntities) {
            for (int i = 0; i < dialogEntities.entitiesByType.size(); i++) {
                int type = dialogEntities.entitiesByType.keyAt(i);
                FileEntities entitesToMerge = dialogEntities.entitiesByType.valueAt(i);
                FileEntities entities = entitiesByType.get(type, null);
                if (entities == null) {
                    entities = new FileEntities();
                    entitiesByType.put(type, entities);
                }
                entities.count += entitesToMerge.count;
                entities.totalSize += entitesToMerge.totalSize;
                totalSize += entitesToMerge.totalSize;
                entities.files.addAll(entitesToMerge.files);
            }
        }
    }

    private static class FileEntities {
        long totalSize;
        int count;
        List<CacheModel.FileInfo> files = new ArrayList<>();
    }


    public Void clearCache() {
        clearDatabase();
        cleanupFolders();


        //TODO: Clear Gallery
        //TODO: Make unlongin
        return null;
    }


    public void addRequest() {
        this.addTask(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                // Do something in background
                return clearCache();
            }
        });
    }

    @Override
    protected void onPostExecute(List<Void> results) {
        FileLog.e("Cache Cleared -> Now Unlogin Account");
    }


}