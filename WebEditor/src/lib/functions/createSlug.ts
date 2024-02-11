export function createSlug(title: string) {
	// const invalid: RegExp = new RegExp(';/?:@&=+$, ', 'g');
	const invalid = /[;\/?:@%&=+$,\(\) ]/g;
	return title.trim().toLowerCase().replaceAll(invalid, '-').replaceAll('--', '-');
}
