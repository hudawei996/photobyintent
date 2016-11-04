package com.example.android.photobyintent;

import java.io.File;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;

public final class FroyoAlbumDirFactory extends AlbumStorageDirFactory {

	@TargetApi(Build.VERSION_CODES.FROYO)
	@Override
	public File getAlbumStorageDir(String albumName) {
		// TODO Auto-generated method stub
		return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), albumName);
	}
}
