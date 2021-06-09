import fs from "fs";
import "path";
import * as Path from "path";

fs.rmdirSync('dist', {recursive: true});

let files = fs.readdirSync('../src/main/java/com/stirante/json/functions/impl/');

files.forEach(value => {
    if (!value.endsWith('.java')) return;
    let content = fs.readFileSync('../src/main/java/com/stirante/json/functions/impl/' + value, 'utf8');
    let regex = /\/\*\*([^]*?)\*\/([^{;/]+)/gm;
    let m, declaration, doc;

    while ((m = regex.exec(content)) !== null) {
        if (m.index === regex.lastIndex) {
            regex.lastIndex++;
        }

        if (typeof m[1] === "string" && m[1] !== null) {
            if (typeof m[2] === "string" && m[2] !== null) {
                declaration = m[2].trim();
                doc = m[1];

                // if the source code line is an import statement
                if (/^import\s+/.test(declaration)) {
                    // ignore this piece
                    continue;
                }

                // if this is a single line comment
                if (doc.indexOf("*") === -1) {
                    // prepend an asterisk to achieve the normal line structure
                    doc = "*" + doc;
                }

                createDoc(value.replace('.java', ''), declaration, doc);
            }
        }
    }
});

function createDoc(fileName, declaration, doc) {
    let path = kebabize(fileName);
    let catName = path.substr(0, 1).toUpperCase() + path.substr(1).replace('-', ' ')
    doc = doc
        // Trim all lines and remove * at the beginning
        .split('\n')
        .map(value => value.trim().replace(/^\*\s?/g, ''))
        .join('\n')
        // Remove code tags
        .replace(/<\s*?code\s*?>(.*?)<\s*?\/\s*?code\s*?>/gs, "```json$1```")
        // Remove pre tags
        .replace(/<\s*?pre\s*?>(.*?)<\s*?\/\s*?pre\s*?>/gs, "`$1`")
        // Fix mustache for jekyll
        .replace('{{', '{{"{{').replace('}}', '}}"}}');
    if (declaration.startsWith('public class ')) {
        path += '/' + 'index.md';
        let output = `---
layout: page
title: ` + catName + `
parent: JSON Templating Engine
has_children: true
---

# ` + catName + `
` + doc;
        write(path, output);
    } else if (declaration.startsWith('@JSONFunction')) {
        // Prepare file name
        let name = declaration
            .replace(/\s+/gm, ' ')
            .replace(/@JSONFunction \w+ \w+ \w+ (\w+)\(.*\)/gm, '$1');
        path += '/' + name + '.md';
        let sections = {
            overview: '',
            args: '',
            example: '',
            deprecated: false
        };
        let lastSection = 'overview';
        doc.split('\n').forEach(value => {
            if (value.startsWith("@param")) {
                lastSection = 'args';
                let s = value.replace(/@param \w+ /, '');
                sections[lastSection] += ' - ' + s.split(':')[0].trim() + ': ' + s.substr(s.split(':')[0].length + 1).trim() + '\n';
            }
            else if (value.startsWith("@example")) {
                lastSection = 'example';
                sections[lastSection] += value.replace(/@example ?/, '') + '\n';
            }
            else if (value.startsWith("@deprecated")) {
                sections.deprecated = true;
            }
            else {
                sections[lastSection] += value + '\n';
            }
        })
        let output = `---
layout: page
grand_parent: JSON Templating Engine
parent: ` + catName + `
title: ` + name + (sections.deprecated ? ' (deprecated)' : '') + `
---

# ` + name + (sections.deprecated ? ' (deprecated)' : '') + `
` + sections.overview + (sections.args.length > 0 ? '\n## Arguments\n\n' + sections.args : '') + (sections.example.length > 0 ? '\n## Example\n' + sections.example : '');
        write(path, output.substr(0, output.length - 1));
    }
}

function write(path, content) {
    let p = 'dist/' + path;
    if (fs.existsSync(p)) {
        console.warn('File ' + p + ' already exists!')
    }
    fs.mkdirSync(Path.dirname(p), {recursive: true});
    fs.writeFileSync(p, content);
}

function kebabize(str) {
    return str.split('').map((letter, idx) => {
        return letter.toUpperCase() === letter
            ? `${idx !== 0 ? '-' : ''}${letter.toLowerCase()}`
            : letter;
    }).join('');
}
