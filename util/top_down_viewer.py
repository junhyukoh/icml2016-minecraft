import matplotlib
import matplotlib.pyplot as plt
from matplotlib import patches
import io
from matplotlib import colors
import xml.dom.minidom
import numpy as np
import collections
from PIL import Image
from PIL import ImageEnhance

class TopDownViewer():
  def __init__(self):
    self.block_mapping = collections.OrderedDict()
    self.default_block_type_id = -1
    self.reset()

  def reset(self):
    plt.clf()
    self.grid = []
    self.player = plt.Circle((0, 0), .25, color='k')
    self.arrow_img =  matplotlib.patches.Arrow(x=0, y=0, dx=1, dy=0, width=1, 
        fill = True, color='k', visible = True)
    self.ax = plt.axes()
    self.ax.xaxis.set_visible(False)
    self.ax.xaxis.set_animated(True)
    self.ax.yaxis.set_visible(False)
    self.ax.yaxis.set_animated(True)

  def initialize(self, block_xml, size):
    self.img_size = size
    DOMTree = xml.dom.minidom.parse(block_xml)
    collection = DOMTree.documentElement
    blockTypes = collection.getElementsByTagName("block")

    for blockType in blockTypes:
        if blockType.hasAttribute("color"):
            colorname = blockType.getAttribute("color").lower().encode("ascii")
            if colorname in colors.cnames:
                for str in blockType.getAttribute("goalID").encode("ascii").split(" "):
                    self.block_mapping[int(str)] = colors.cnames.get(colorname)
            else:
                print("Color " + blockType.getAttribute("color") \
                    + " not found in matplotlib's cnames!")
                exit(-1)
        else:
            self.block_mapping[0] = colors.cnames.get("gray")
            assert self.default_block_type_id == -1, "Default block type defined twice!"
            self.default_block_type_id = 0
    self.block_mapping[len(self.block_mapping) + 1] = colors.cnames.get("white")

  def draw_topology(self, topology_csv):
    self.grid = np.loadtxt(topology_csv, delimiter=',')
    for x in range(0, self.grid.shape[0]):
        for y in range(0, self.grid.shape[1]):
            if self.grid[x, y] != 1:
                self.grid[x, y] = self.default_block_type_id
    self.ax.set_xlim([0, self.grid.shape[1]])
    self.ax.set_ylim([self.grid.shape[0], 0])

  def draw_goal_block(self, goal_csv):
    goalblocksgrid = np.genfromtxt(goal_csv, delimiter=',', filling_values="0")
    for x in range(0, self.grid.shape[0]):
        for y in range(0, self.grid.shape[1]):
            if self.grid[x, y] == 1 and goalblocksgrid[x, y] == 0:
                self.grid[x, y] = len(self.block_mapping)
            else:
                self.grid[x, y] = goalblocksgrid[x, y]
    self.ax.pcolormesh(self.grid, cmap=colors.ListedColormap( \
        [v for k, v in sorted(self.block_mapping.items(), key=lambda t: t[0])]), 
        edgecolors=colors.cnames.get("black"))
    self.ax.add_artist(self.player)
    self.ax.add_artist(self.arrow_img)

  def update_frame(self, pos_x, pos_y, facing, cur_frame):
    cur_img = Image.fromarray(cur_frame, "RGB")
    cur_img = cur_img.transpose(Image.FLIP_LEFT_RIGHT)
    enhancer = ImageEnhance.Brightness(cur_img)
    cur_img = enhancer.enhance(2)
    img_size = int(self.img_size)
    self.player.center = (pos_x + 0.5, pos_y + 0.5)
    self.arrow_img.remove()
    self.arrow_img.set_visible(False)

    if facing == 0:
        Dx = 1
        Dy = 0
    elif facing == 1:
        Dx = 0
        Dy = -1
    elif facing == 2:
        Dx = -1
        Dy = 0
    elif facing == 3:
        Dx = 0
        Dy = 1
    else:
        assert False

    self.arrow_img = matplotlib.patches.Arrow(x = pos_x + 0.5, 
        y = pos_y + 0.5, dx = Dx, dy = Dy, width = 1.0, fill = True, 
        color='k', visible = True)
    self.ax.add_artist(self.arrow_img)
    buf = io.BytesIO()
    plt.savefig(buf, format='png', bbox_inches='tight', transparent="True", pad_inches=0)
    buf.seek(0)
    top_down_view = Image.open(buf)
    top_down_view = top_down_view.convert(mode="RGB")
    if self.grid.shape[0] >= self.grid.shape[1]:
      top_down_view_resized = top_down_view.resize(( \
        int(img_size * float(self.grid.shape[1])/float(self.grid.shape[0])), img_size),
        Image.ANTIALIAS)
    else:
      top_down_view_resized = top_down_view.resize(( \
          img_size, int(img_size * float(self.grid.shape[0])/float(self.grid.shape[1]))),
          Image.ANTIALIAS)

    top_down_view = top_down_view.resize(( \
        img_size, int(img_size * float(self.grid.shape[0])/float(self.grid.shape[1]))),
        Image.ANTIALIAS)

    off_x = (img_size - top_down_view_resized.size[0]) / 2
    off_y = (img_size - top_down_view_resized.size[1]) / 2
    side_by_side = Image.new("RGB", (img_size * 2, img_size))
    side_by_side.paste(cur_img, (0, 0))
    side_by_side.paste(top_down_view_resized, (img_size + off_x, off_y))

    full_img = np.asarray(side_by_side)
    top_down_img = np.asarray(top_down_view)
    return full_img, top_down_img, np.asarray(cur_img)

def create_viewer():
  return TopDownViewer()
