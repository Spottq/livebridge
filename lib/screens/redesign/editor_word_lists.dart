List<String> editorWords(String text) => text
    .split('\n')
    .map((v) => v.trim())
    .where((v) => v.isNotEmpty)
    .toSet()
    .toList();

bool validEditorWords(List<String> words) =>
    words.length <= 100 && words.every((v) => v.length <= 200);
