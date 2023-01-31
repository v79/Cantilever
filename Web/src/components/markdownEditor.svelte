<div class="mt-5 md:mt-0 md:col-span-2">
	<h3 class="px-4 py-4 text-2xl font-bold">Markdown Editor</h3>
	<form action="#" method="POST">
		<div class="shadow overflow-hidden sm:rounded-md">
			<div class="px-4 py-5 sm:p-6">
				<div class="grid grid-cols-6 gap-6">
					<div class="col-span-6 sm:col-span-6 lg:col-span-2">
						<label for="Slug" class="block text-sm font-medium text-slate-200">Slug</label>
						<input
							type="text"
							name="Slug"
							id="Slug"
							class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
						/>
					</div>

					<div class="col-span-6 sm:col-span-3 lg:col-span-2">
						<label for="date" class="block text-sm font-medium text-slate-200">Date</label>
						<input
							type="date"
							name="date"
							id="date"
							class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
						/>
					</div>

					<!-- <div class="col-span-6 sm:col-span-3 lg:col-span-2">
				  <label for="postal_code" class="block text-sm font-medium text-gray-700">ZIP / Postal</label>
				  <input type="text" name="postal_code" id="postal_code" autocomplete="postal-code" class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md">
				</div> -->

					<div class="col-span-6">
						<label for="title" class="block text-sm font-medium text-slate-200">Title</label>
						<input
							type="text"
							name="title"
							id="title"
							autocomplete="title"
							class="mt-1 focus:ring-indigo-500 focus:border-indigo-500 block w-full shadow-sm sm:text-sm border-gray-300 rounded-md"
						/>
					</div>
					<div class="col-span-6">
						<label for="markdown" class="block text-sm font-medium text-slate-200">Markdown</label>
						<textarea name="markdown" id="markdown" class="mt-1 textarea-lg  focus:ring-indigo-500 focus:border-indigo-500 block w-full rounded-md" placeholder="Markdown goes here" />
					</div>
				</div>
			</div>
			<!-- <div class="px-4 py-3 text-right sm:px-6">
				<button
					type="submit"
					class="inline-flex justify-center py-2 px-4 border border-transparent shadow-sm text-sm font-medium rounded-md text-white bg-indigo-600 hover:bg-indigo-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500"
				>
					Save
				</button>
			</div> -->
		</div>
	</form>
</div>
