import torch
import ultralytics
from ultralytics import YOLO

def convert_yolo_to_onnx(weights_path, output_path="yolo11s.onnx", opset_version=17, simplify=True):
    """
    Converts a YOLO model from Ultralytics to ONNX format.

    Args:
        weights_path (str): Path to the YOLO weights file (.pt).
        output_path (str, optional): Path to save the ONNX model. Defaults to "yolo11s.onnx".
        opset_version (int, optional): ONNX opset version. Defaults to 17.
        simplify (bool, optional): Whether to simplify the ONNX model. Defaults to True.
    """
    try:
        # Load the YOLO model
        model = YOLO(weights_path)

        # Dummy input for tracing (adjust shape if needed)
        dummy_input = torch.randn(1, 3, 640, 640) #Typical yolo input shape

        # Export to ONNX
        model.model.eval() #set the model to evaluation mode.
        torch.onnx.export(
            model.model,
            dummy_input,
            output_path,
            opset_version=opset_version,
            input_names=['images'],
            output_names=['output'],
            dynamic_axes={'images': {0: 'batch', 2: 'height', 3: 'width'}, 'output': {0: 'batch'}},
            do_constant_folding=True,
        )

        print(f"YOLO model converted to ONNX and saved to {output_path}")

        if simplify:
            import onnxsim
            onnx_model = onnxsim.simplify(output_path)[0]
            torch.onnx.save(torch.onnx.load(output_path),output_path)
            print("ONNX model simplified.")

    except Exception as e:
        print(f"Error during ONNX conversion: {e}")

if __name__ == "__main__":
    weights_file = "yolo11s.pt" # Replace with your weights path if different
    convert_yolo_to_onnx(weights_file)