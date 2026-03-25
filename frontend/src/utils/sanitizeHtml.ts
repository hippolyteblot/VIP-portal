export function sanitizeHtml(input: string): string {
  if (!input) {
    return ''
  }

  const parser = new DOMParser()
  const doc = parser.parseFromString(input, 'text/html')

  // Remove dangerous elements entirely.
  doc.querySelectorAll('script, iframe, object, embed').forEach((node) => node.remove())

  // Remove event handler attributes and javascript: URLs.
  doc.querySelectorAll('*').forEach((element) => {
    const attributes = Array.from(element.attributes)
    for (const attribute of attributes) {
      const attrName = attribute.name.toLowerCase()
      const attrValue = attribute.value.trim().toLowerCase()

      if (attrName.startsWith('on')) {
        element.removeAttribute(attribute.name)
        continue
      }

      if ((attrName === 'href' || attrName === 'src') && attrValue.startsWith('javascript:')) {
        element.removeAttribute(attribute.name)
      }
    }
  })

  return doc.body.innerHTML
}
