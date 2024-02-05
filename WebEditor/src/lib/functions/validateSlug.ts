const validChars: RegExp = /^[\a-zA-Z0-9]+(?:-[\w]+)*$/;

/**
 * Validate slug/srcKey - requires at least one character, and only allows letters, numbers, and hyphens
 * @param slug
 * @returns
 */
export function validateSlug(slug: string): boolean {
	return validChars.test(slug);
}
