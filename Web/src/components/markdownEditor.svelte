<form class="form-control">
	<div class="flex flex-row flex-wrap w-full max-w-xs">
		<div class="flex mb-4">
			<!-- title -->
			<div class="w-1/4">
				<label for="mdTitle" class="label justify-end px-4">
					<span class="label-text">Title</span>
				</label>
			</div>
			<div class="w-3/4">
				<input id="mdTitle" type="text" placeholder="Title" class="input input-bordered" />
			</div>
			<!-- date -->
			<div class="w-1/4">
				<label for="mdDate" class="label justify-end px-4">
					<span class="label-text">Date</span>
				</label>
			</div>
			<div class="w-3/4">
				<input id="mdDate" type="text" placeholder="Date" class="input input-bordered" />
			</div>
		</div>
	</div>
	<div class="flex flex-row flex-nowrap w-full max-w-xs">
		<!-- slug or url -->
		<div class="w-1/4">
			<label for="mdSlug" class="label justify-end px-4">
				<span class="label-text">Slug</span>
			</label>
		</div>
		<div class="w-3/4">
			<input id="mdSlug" type="text" placeholder="URL" class="input input-bordered" disabled />
		</div>
	</div>
	<textarea class="textarea-lg w-full max-w-xs" placeholder="Markdown source goes here" />
</form>
