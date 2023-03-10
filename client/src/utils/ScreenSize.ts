import {useEffect, useState} from "react";

export class ScreenSize {
    private size: Rect
    get isPortrait(): boolean {
        return this.size.width < this.size.height;
    }

    get isPhone(): boolean {
        return this.size.width < 920;
    }
    constructor(size: Rect) {
        this.size = size;
    }

    static ofDocument(document: Document) {
        return new ScreenSize(document.body.getBoundingClientRect());
    }
}

export function useScreenSize(): ScreenSize {
    const [screenSize, setScreenSize] = useState(ScreenSize.ofDocument(document))

    useEffect(() => {
        function handleResize() {
            setScreenSize(ScreenSize.ofDocument(document))
        }

        window.addEventListener('resize', handleResize)

        return () => window.removeEventListener('resize', handleResize)
    })
    return screenSize;
}

export interface Rect {
    top: number;
    left: number;
    height: number;
    width: number;
}
