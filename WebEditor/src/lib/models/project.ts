/*
	The [CantileverProject] represents some metadata about the entire project.
*/
export class CantileverProject {
	projectName: string;
	author: string;
	dateFormat: string;
	dateTimeFormat: string;
	imageResolutions: Map<string, ImgRes>;
	attributes: Map<string, string>;
	domain: string;

	constructor(
		projectName: string,
		author: string,
		dateFormat: string,
		dateTimeFormat: string,
		imageResolutions: Map<string, ImgRes>,
		attributes: Map<string, string>,
		domain: string
	) {
		this.projectName = projectName;
		this.author = author;
		this.dateFormat = dateFormat;
		this.dateTimeFormat = dateTimeFormat;
		this.imageResolutions = imageResolutions;
		this.attributes = attributes;
		this.domain = domain;
	}
}


/*
	The [ImgRes] Represents an image resolution in pixels.
*/
export class ImgRes {
	w: number | undefined;
	h: number | undefined;

	constructor(width: number, height: number) {
		this.w = width;
		this.h = height;
	}

	toJSON(): string {
		return this.getStringW() + 'x' + this.getStringH();
	}

	getStringW(): string {
		if (isNaN(this.w!!)) {
			return '';
		} else {
			return '' + this.w;
		}
	}
	getStringH(): string {
		if (isNaN(this.h!!)) {
			return '';
		} else {
			return '' + this.h;
		}
	}
}

/**
 * Convert a string like "640x480" into a [ImgRes] object with values w=640, h=480
 * @param resString
 * @returns an [ImgRes] object with the appropriate dimensions. If a dimension is not found, it will be returned as NaN.
 */
export function parseResString(resString: string) {
	const wS: string = resString.substring(0, resString.indexOf('x'));
	const hS: string = resString.substring(resString.indexOf('x') + 1);
	return new ImgRes(parseInt(wS), parseInt(hS));
}