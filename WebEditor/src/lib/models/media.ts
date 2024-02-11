/**
 * An image file may have in one of several different formats, and may have multiple resolutions
 */
export class ImageNode {
	srcKey: string;
	lastUpdated: Date;
	url: string | undefined; // url not relevant
    contentType: string;
	hasBeenPlaced: boolean = false;

	constructor(srcKey: string, lastUpdated: Date, url: string, contentType: string, hasBeenPlaced: boolean) {
		this.srcKey = srcKey;
		this.lastUpdated = lastUpdated;
        this.contentType = contentType;
		this.url = url;
		this.hasBeenPlaced = hasBeenPlaced;
	}

	shortName(): string {
		return this.srcKey.split('/').slice(-1).join();
	}
}

/**
 * Raw image data, used for fetching images from S3
 */
export class ImageDTO {
	srcKey: string;
	contentType: string;
	bytes: Blob | string;

	constructor(key: string, contentType: string, bytes: Blob | string) {
		this.srcKey = key;
		this.contentType = contentType;
		this.bytes = bytes;
	}
}

export interface ImageList {
    count: number;
    lastUpdated: Date;
    images: ImageNode[];
}