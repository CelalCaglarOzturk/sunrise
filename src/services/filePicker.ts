import { Filesystem, Directory } from '@capacitor/filesystem';

export class FilePickerService {
  private static instance: FilePickerService;

  private constructor() {}

  static getInstance(): FilePickerService {
    if (!FilePickerService.instance) {
      FilePickerService.instance = new FilePickerService();
    }
    return FilePickerService.instance;
  }

  /**
   * Request permissions to read files
   */
  async requestPermissions(): Promise<boolean> {
    try {
      const result = await Filesystem.requestPermissions();
      return result.publicStorage === 'granted';
    } catch (error) {
      console.error('Error requesting file permissions:', error);
      return false;
    }
  }

  /**
   * Check if we have file permissions
   */
  async checkPermissions(): Promise<boolean> {
    try {
      const result = await Filesystem.checkPermissions();
      return result.publicStorage === 'granted';
    } catch (error) {
      console.error('Error checking file permissions:', error);
      return false;
    }
  }

  /**
   * Pick an audio file from device storage
   * Note: This is a simplified version. In a real app, you would use
   * Capacitor's File Picker plugin or native file chooser
   */
  async pickAudioFile(): Promise<{ name: string; uri: string } | null> {
    try {
      // Check permissions first
      const hasPermission = await this.checkPermissions();
      if (!hasPermission) {
        const granted = await this.requestPermissions();
        if (!granted) {
          return null;
        }
      }

      // In a real implementation, you would use a native file picker
      // For now, we'll return a mock implementation
      // You would typically use Capacitor's File Picker plugin or
      // integrate with Android's Intent to pick files

      // This is a placeholder that should be replaced with actual file picker
      // when running on device. For web testing, we can use the File API.
      return await this.openFilePicker();
    } catch (error) {
      console.error('Error picking audio file:', error);
      return null;
    }
  }

  /**
   * Open native file picker (web fallback)
   */
  private async openFilePicker(): Promise<{ name: string; uri: string } | null> {
    return new Promise((resolve) => {
      // Create a hidden file input
      const input = document.createElement('input');
      input.type = 'file';
      input.accept = 'audio/*';
      input.style.display = 'none';

      input.onchange = (e: Event) => {
        const target = e.target as HTMLInputElement;
        const file = target.files?.[0];

        if (file) {
          // Create a blob URL for the file
          const uri = URL.createObjectURL(file);
          resolve({
            name: file.name,
            uri: uri,
          });
        } else {
          resolve(null);
        }

        // Clean up
        document.body.removeChild(input);
      };

      input.oncancel = () => {
        document.body.removeChild(input);
        resolve(null);
      };

      document.body.appendChild(input);
      input.click();
    });
  }

  /**
   * Get common audio file paths on Android
   * This would be used to show a list of audio files to choose from
   */
  async getAudioFiles(): Promise<Array<{ name: string; path: string }>> {
    try {
      const hasPermission = await this.checkPermissions();
      if (!hasPermission) {
        return [];
      }

      // In a real app, you would scan common audio directories
      // For example: Music, Ringtones, Notifications, Alarms
      // This requires native implementation or a Capacitor plugin

      // Return some default system sounds that are typically available
      return [
        { name: 'Default', path: '' },
        { name: 'Gentle', path: 'gentle.mp3' },
        { name: 'Loud', path: 'loud.mp3' },
        { name: 'Nature', path: 'nature.mp3' },
        { name: 'Digital', path: 'digital.mp3' },
      ];
    } catch (error) {
      console.error('Error getting audio files:', error);
      return [];
    }
  }

  /**
   * Validate if a file is an audio file
   */
  isAudioFile(filename: string): boolean {
    const audioExtensions = [
      '.mp3', '.wav', '.ogg', '.m4a', '.aac',
      '.flac', '.wma', '.aiff', '.opus'
    ];

    const lowerFilename = filename.toLowerCase();
    return audioExtensions.some(ext => lowerFilename.endsWith(ext));
  }

  /**
   * Get file size in human-readable format
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}

export const filePickerService = FilePickerService.getInstance();
