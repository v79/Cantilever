// AppStatus is a dumping ground for things which need to hang around

import type { FileType, FolderNode, Page, Post, Template } from './structure';

export interface AppStatus {
	activeFile: string;
	isNewFile: boolean;
	hasChanged: boolean;
	isValid: boolean;
	currentPage: string;
	newSlug: string;
	fileType: FileType | null;
	folder: FolderNode | null;
}
